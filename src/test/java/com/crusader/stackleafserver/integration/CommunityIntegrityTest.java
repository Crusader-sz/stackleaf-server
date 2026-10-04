package com.crusader.stackleafserver.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** 使用真实 MySQL 事务及 Redis 会话验证权限、关联校验、并发删除；只清理本测试创建的数据。 */
@SpringBootTest
@AutoConfigureMockMvc
class CommunityIntegrityTest {
    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper json;
    private final List<String> tokens = new ArrayList<>();
    private long owner, other, admin, category, tag, published, draft, hidden;
    private String ownerToken, otherToken, adminToken;
    private String prefix;
    private final List<Long> createdUsers = new ArrayList<>();

    private long insert(String table, Map<String, ?> values) {
        return new SimpleJdbcInsert(jdbc).withTableName(table).usingGeneratedKeyColumns("id")
                .usingColumns(values.keySet().toArray(String[]::new)).executeAndReturnKey(values).longValue();
    }

    @BeforeEach
    void setup() throws Exception {
        prefix = "check_" + UUID.randomUUID().toString().substring(0, 8);
        String password = new BCryptPasswordEncoder().encode("testpass123");
        owner = insert("user", Map.of("username", prefix + "_owner", "password", password, "nickname", "owner", "role", 0));
        other = insert("user", Map.of("username", prefix + "_other", "password", password, "nickname", "other", "role", 0));
        admin = insert("user", Map.of("username", prefix + "_admin", "password", password, "nickname", "admin", "role", 1));
        category = insert("category", Map.of("name", prefix));
        tag = insert("tag", Map.of("name", prefix));
        published = article(1);
        draft = article(0);
        hidden = article(2);
        insert("article_tag", Map.of("article_id", published, "tag_id", tag));
        ownerToken = login("owner"); otherToken = login("other"); adminToken = login("admin");
    }

    private long article(int status) {
        return insert("article", Map.of("author_id", owner, "category_id", category,
                "title", prefix + "_" + status, "content", "test content", "status", status));
    }

    private String login(String suffix) throws Exception {
        JsonNode result = request("POST", "/user/login", null,
                Map.of("username", prefix + "_" + suffix, "password", "testpass123"));
        assertEquals(200, result.path("code").asInt());
        String token = result.path("data").asText();
        assertFalse(token.isBlank()); tokens.add(token); return token;
    }

    private JsonNode request(String method, String path, String token, Object body) throws Exception {
        MockHttpServletRequestBuilder req = switch (method) {
            case "GET" -> get(path);
            case "POST" -> post(path);
            case "PUT" -> put(path);
            case "DELETE" -> delete(path);
            default -> throw new IllegalArgumentException(method);
        };
        if (token != null) req.header("Authorization", token);
        if (body != null) req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body));
        return json.readTree(mvc.perform(req).andReturn().getResponse().getContentAsString());
    }

    private int code(String method, String path, String token, Object body) throws Exception {
        return request(method, path, token, body).path("code").asInt();
    }

    @AfterEach
    void cleanup() throws Exception {
        try {
            for (String token : tokens) request("POST", "/user/logout", token, null);
        } finally {
            for (String table : List.of("comment", "article_tag", "article_like", "article_favorite")) {
                jdbc.update("DELETE FROM " + table + " WHERE article_id IN (SELECT id FROM article WHERE author_id IN (?, ?))", owner, other);
            }
            jdbc.update("DELETE FROM article WHERE author_id IN (?, ?)", owner, other);
            jdbc.update("DELETE FROM category WHERE id = ? OR name = ?", category, prefix + "_created");
            jdbc.update("DELETE FROM tag WHERE id = ? OR name = ?", tag, prefix + "_created");
            jdbc.update("DELETE FROM user_follow WHERE user_id IN (?, ?, ?) OR follow_user_id IN (?, ?, ?)", owner, other, admin, owner, other, admin);
            jdbc.update("DELETE FROM user WHERE id IN (?, ?, ?)", owner, other, admin);
            for (Long id : createdUsers) jdbc.update("DELETE FROM user WHERE id = ?", id);
        }
    }

    @Test
    void publicListsAlwaysExcludeDraftsAndHiddenArticles() throws Exception {
        for (String status : List.of("", "&status=0", "&status=2")) {
            JsonNode result = request("GET", "/article/page?categoryId=" + category + status, null, null);
            assertEquals(200, result.path("code").asInt());
            assertEquals(1, result.path("data").path("total").asInt());
            assertEquals(published, result.path("data").path("records").get(0).path("id").asLong());
        }
        assertEquals(404, code("GET", "/article/" + draft, null, null));
        assertEquals(404, code("GET", "/article/" + hidden, ownerToken, null));
        assertEquals(0, jdbc.queryForObject("SELECT view_count FROM article WHERE id = ?", Integer.class, draft));
        assertEquals(200, code("GET", "/article/" + published, null, null));
    }

    @Test
    void privateReadsRequireOwnershipOrAdminRole() throws Exception {
        assertEquals(401, code("GET", "/article/mine/" + draft, null, null));
        assertEquals(200, code("GET", "/article/mine/" + draft, ownerToken, null));
        assertEquals(404, code("GET", "/article/mine/" + draft, otherToken, null));
        assertEquals(403, code("GET", "/article/admin/" + draft, ownerToken, null));
        assertEquals(200, code("GET", "/article/admin/" + draft, adminToken, null));
        JsonNode mine = request("GET", "/article/mine/page?status=0", ownerToken, null);
        assertEquals(1, mine.path("data").path("total").asInt());
        assertEquals(403, code("GET", "/article/admin/page", otherToken, null));
        assertEquals(3, request("GET", "/article/admin/page?categoryId=" + category, adminToken, null).path("data").path("total").asInt());
    }

    @Test
    void anonymousWritesAreIntercepted() throws Exception {
        assertEquals(401, code("DELETE", "/article/" + published, null, null));
        assertEquals(401, code("POST", "/category", null, Map.of("name", prefix)));
        assertEquals(401, code("DELETE", "/tag/" + tag, null, null));
    }

    @Test
    void taxonomyWritesRequireAdminForEveryMethod() throws Exception {
        for (String domain : List.of("category", "tag")) {
            long id = domain.equals("category") ? category : tag;
            Map<String, Object> body = Map.of("id", id, "name", prefix);
            assertEquals(403, code("POST", "/" + domain, ownerToken, body));
            assertEquals(403, code("PUT", "/" + domain, ownerToken, body));
            assertEquals(403, code("DELETE", "/" + domain + "/" + id, ownerToken, null));
            assertEquals(200, code("PUT", "/" + domain, adminToken, body));
        }
    }

    @Test
    void adminsCanCreateAndDeleteTaxonomy() throws Exception {
        for (String domain : List.of("category", "tag")) {
            assertEquals(200, code("POST", "/" + domain, adminToken, Map.of("name", prefix + "_created")));
            Long id = jdbc.queryForObject("SELECT id FROM " + domain + " WHERE name = ?", Long.class, prefix + "_created");
            assertEquals(200, code("DELETE", "/" + domain + "/" + id, adminToken, null));
        }
    }

    @Test
    void articleDeletionRequiresOwnerOrAdmin() throws Exception {
        assertEquals(403, code("DELETE", "/article/" + draft, otherToken, null));
        assertEquals(200, code("DELETE", "/article/" + draft, ownerToken, null));
        assertEquals(200, code("DELETE", "/article/" + hidden, adminToken, null));
    }

    @Test
    void authorsCannotSetTopOrBypassTakedown() throws Exception {
        assertEquals(403, code("PUT", "/article", ownerToken, Map.of("id", published, "isTop", 1)));
        assertEquals(403, code("PUT", "/article", ownerToken, Map.of("id", published, "status", 2)));
        assertEquals(403, code("PUT", "/article", ownerToken, Map.of("id", hidden, "status", 1)));
        assertEquals(403, code("PUT", "/article", otherToken, Map.of("id", published, "title", "changed")));
        assertEquals(200, code("PUT", "/article", adminToken, Map.of("id", published, "isTop", 1, "status", 2)));
        assertEquals(404, code("GET", "/article/" + published, null, null));
        assertEquals(200, code("PUT", "/article", adminToken, Map.of("id", hidden, "status", 1)));
    }

    @Test
    void partialEditsPreserveFieldsAndTagsButEmptyListClearsTags() throws Exception {
        assertEquals(200, code("PUT", "/article", ownerToken, Map.of("id", published, "title", "new title")));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM article_tag WHERE article_id = ?", Integer.class, published));
        assertEquals(1, jdbc.queryForObject("SELECT status FROM article WHERE id = ?", Integer.class, published));
        assertEquals("test content", jdbc.queryForObject("SELECT content FROM article WHERE id = ?", String.class, published));
        assertEquals(200, code("PUT", "/article", ownerToken, Map.of("id", published, "tagIds", List.of())));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM article_tag WHERE article_id = ?", Integer.class, published));
    }

    @Test
    void referencesAreValidatedAndDuplicateTagsAreDeduplicated() throws Exception {
        assertEquals(400, code("PUT", "/article", ownerToken, Map.of("id", published, "categoryId", -1)));
        assertEquals(400, code("PUT", "/article", ownerToken, Map.of("id", published, "tagIds", List.of(-1))));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM article_tag WHERE article_id = ?", Integer.class, published));
        assertEquals(200, code("PUT", "/article", ownerToken, Map.of("id", published, "tagIds", List.of(tag, tag))));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM article_tag WHERE article_id = ?", Integer.class, published));
        Map<String, Object> body = new HashMap<>(Map.of("title", "test", "content", "body", "categoryId", category));
        body.put("tagIds", List.of(-1));
        assertEquals(400, code("POST", "/article", ownerToken, body));
        body.put("tagIds", List.of(tag, tag));
        assertEquals(200, code("POST", "/article", ownerToken, body));
    }

    private long comment(long article, Long parent, String token) throws Exception {
        Map<String, Object> body = new HashMap<>(Map.of("articleId", article, "content", "test comment"));
        if (parent != null) body.put("parentId", parent);
        JsonNode result = request("POST", "/comment", token, body);
        assertEquals(200, result.path("code").asInt());
        return result.path("data").asLong();
    }

    @Test
    void commentsValidateArticleParentAndReplyTarget() throws Exception {
        assertEquals(404, code("POST", "/comment", ownerToken, Map.of("articleId", -1, "content", "test")));
        assertEquals(404, code("POST", "/comment", ownerToken, Map.of("articleId", draft, "content", "test")));
        long parent = comment(published, null, ownerToken);
        long second = article(1);
        assertEquals(400, code("POST", "/comment", otherToken, Map.of("articleId", second, "parentId", parent, "content", "test")));
        assertEquals(400, code("POST", "/comment", otherToken, Map.of("articleId", published, "parentId", -1, "content", "test")));
        assertEquals(400, code("POST", "/comment", otherToken, Map.of("articleId", published, "parentId", parent, "replyUserId", other, "content", "test")));
        long child = comment(published, parent, otherToken);
        assertEquals(owner, jdbc.queryForObject("SELECT reply_user_id FROM comment WHERE id = ?", Long.class, child));
        jdbc.update("UPDATE comment SET status = 0 WHERE id = ?", parent);
        assertEquals(400, code("POST", "/comment", otherToken, Map.of("articleId", published, "parentId", parent, "content", "test")));
        assertEquals(2, jdbc.queryForObject("SELECT comment_count FROM article WHERE id = ?", Integer.class, published));
    }

    @Test
    void publicCommentAndInteractionEndpointsCannotExposeHiddenArticles() throws Exception {
        long parent = comment(published, null, ownerToken);
        insert("article_favorite", Map.of("article_id", published, "user_id", owner));
        jdbc.update("UPDATE article SET status = 2 WHERE id = ?", published);
        assertEquals(404, code("GET", "/comment/top?articleId=" + published, null, null));
        assertEquals(404, code("GET", "/comment/children/" + parent, null, null));
        assertEquals(404, code("POST", "/article/like/" + published, otherToken, null));
        assertEquals(404, code("POST", "/article/favorite/" + published, otherToken, null));
        assertEquals(0, request("GET", "/user/favorites?userId=" + owner, ownerToken, null).path("data").size());
    }

    @Test
    void commentDeletionCascadesAndKeepsCountNonnegative() throws Exception {
        long parent = comment(published, null, ownerToken);
        long child = comment(published, parent, otherToken);
        comment(published, child, ownerToken);
        assertEquals(403, code("DELETE", "/comment/" + parent, otherToken, null));
        assertEquals(404, code("DELETE", "/comment/-1", ownerToken, null));
        assertEquals(200, code("DELETE", "/comment/" + parent, ownerToken, null));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM comment WHERE article_id = ?", Integer.class, published));
        assertEquals(0, jdbc.queryForObject("SELECT comment_count FROM article WHERE id = ?", Integer.class, published));
        assertEquals(404, code("DELETE", "/comment/" + child, otherToken, null));
    }

    @Test
    void concurrentDeletesDoNotDoubleDecrement() throws Exception {
        long parent = comment(published, null, ownerToken);
        long child = comment(published, parent, otherToken);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Integer> first = pool.submit(() -> { start.await(); return code("DELETE", "/comment/" + parent, ownerToken, null); });
            Future<Integer> second = pool.submit(() -> { start.await(); return code("DELETE", "/comment/" + child, otherToken, null); });
            start.countDown();
            assertEquals(200, first.get(15, TimeUnit.SECONDS));
            assertTrue(Set.of(200, 404).contains(second.get(15, TimeUnit.SECONDS)));
            assertEquals(0, jdbc.queryForObject("SELECT comment_count FROM article WHERE id = ?", Integer.class, published));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM comment WHERE article_id = ?", Integer.class, published));
        } finally { pool.shutdownNow(); }
    }
    @Test
    void concurrentReplyAndDeletionLeaveNoOrphans() throws Exception {
        long parent = comment(published, null, ownerToken);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Integer> deletion = pool.submit(() -> { start.await(); return code("DELETE", "/comment/" + parent, ownerToken, null); });
            Future<Integer> reply = pool.submit(() -> { start.await(); return code("POST", "/comment", otherToken,
                    Map.of("articleId", published, "parentId", parent, "content", "concurrent reply")); });
            start.countDown();
            assertEquals(200, deletion.get(15, TimeUnit.SECONDS));
            assertTrue(Set.of(200, 400).contains(reply.get(15, TimeUnit.SECONDS)));
            assertEquals(0, jdbc.queryForObject("SELECT comment_count FROM article WHERE id = ?", Integer.class, published));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM comment WHERE article_id = ?", Integer.class, published));
        } finally { pool.shutdownNow(); }
    }

    @Test
    void adminSessionAndUserManagementRejectOrdinaryAccounts() throws Exception {
        assertEquals(401, code("GET", "/admin/users", null, null));
        assertEquals(403, code("GET", "/admin/session", ownerToken, null));
        assertEquals(403, code("GET", "/admin/users", ownerToken, null));
        assertEquals(403, code("POST", "/admin/users", ownerToken,
                Map.of("username", prefix + "_new", "password", "testpass123", "nickname", "test")));
        assertEquals(403, code("PUT", "/admin/users/" + other, ownerToken,
                Map.of("nickname", "test", "role", 0, "status", 1)));
        assertEquals(403, code("DELETE", "/admin/users/" + other, ownerToken, null));
        JsonNode session = request("GET", "/admin/session", adminToken, null);
        assertEquals(200, session.path("code").asInt());
        assertEquals(admin, session.path("data").path("user").path("id").asLong());
        assertTrue(session.path("data").path("timeoutSeconds").asLong() > 0);
        JsonNode result = request("GET", "/admin/users?keyword=" + prefix + "&pageSize=1&pageNum=2", adminToken, null);
        assertEquals(3, result.path("data").path("total").asInt());
        assertEquals(1, result.path("data").path("records").size());
        assertFalse(result.path("data").path("records").get(0).has("password"));
        assertEquals(400, code("GET", "/admin/users?pageSize=101", adminToken, null));
    }

    @Test
    void adminUserCrudPersistsAndPasswordBlankDoesNotOverwrite() throws Exception {
        Map<String, Object> create = Map.of("username", prefix + "_new", "password", "testpass123",
                "nickname", "new", "email", prefix + "@example.com", "role", 0, "status", 1);
        JsonNode result = request("POST", "/admin/users", adminToken, create);
        assertEquals(200, result.path("code").asInt());
        long id = result.path("data").asLong(); createdUsers.add(id);
        String originalHash = jdbc.queryForObject("SELECT password FROM user WHERE id = ?", String.class, id);
        assertNotEquals("testpass123", originalHash);
        assertEquals(409, code("POST", "/admin/users", adminToken, create));
        assertEquals(200, code("PUT", "/admin/users/" + id, adminToken,
                Map.of("nickname", "updated", "email", "", "role", 0, "status", 1)));
        assertEquals("updated", jdbc.queryForObject("SELECT nickname FROM user WHERE id = ?", String.class, id));
        assertNull(jdbc.queryForObject("SELECT email FROM user WHERE id = ?", String.class, id));
        assertEquals(originalHash, jdbc.queryForObject("SELECT password FROM user WHERE id = ?", String.class, id));
        assertEquals(200, code("DELETE", "/admin/users/" + id, adminToken, null));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM user WHERE id = ?", Integer.class, id));
    }

    @Test
    void adminUsersCannotDeleteSelfOrUsersWithContentAndDisableRevokesSession() throws Exception {
        assertEquals(400, code("DELETE", "/admin/users/" + admin, adminToken, null));
        assertEquals(400, code("PUT", "/admin/users/" + admin, adminToken,
                Map.of("nickname", "admin", "role", 0, "status", 1)));
        assertEquals(409, code("DELETE", "/admin/users/" + owner, adminToken, null));
        assertEquals(200, code("PUT", "/admin/users/" + other, adminToken,
                Map.of("nickname", "disabled", "role", 0, "status", 0)));
        assertEquals(401, code("GET", "/user/me", otherToken, null));
        assertEquals(403, code("POST", "/user/login", null,
                Map.of("username", prefix + "_other", "password", "testpass123")));
    }

    @Test
    void ordinaryProfileEditsKeepSessionAndPrivilegedFields() throws Exception {
        jdbc.update("UPDATE user SET email = ? WHERE id = ?", prefix + "@example.com", other);
        String password = jdbc.queryForObject("SELECT password FROM user WHERE id = ?", String.class, other);
        assertEquals(200, code("PUT", "/admin/users/" + other, adminToken,
                Map.of("nickname", "updated", "email", prefix + "@example.com", "role", 0, "status", 1)));
        assertEquals(200, code("GET", "/user/me", otherToken, null));
        assertEquals(200, code("PUT", "/user/profile", otherToken, Map.of("nickname", "profile", "role", 1, "status", 0)));
        assertEquals(0, jdbc.queryForObject("SELECT role FROM user WHERE id = ?", Integer.class, other));
        assertEquals(1, jdbc.queryForObject("SELECT status FROM user WHERE id = ?", Integer.class, other));
        assertEquals(prefix + "@example.com", jdbc.queryForObject("SELECT email FROM user WHERE id = ?", String.class, other));
        assertEquals(password, jdbc.queryForObject("SELECT password FROM user WHERE id = ?", String.class, other));
    }

    @Test
    void actualRoleDemotionRevokesAdminSession() throws Exception {
        jdbc.update("UPDATE user SET role = 1 WHERE id = ?", other);
        assertEquals(200, code("GET", "/admin/session", otherToken, null));
        assertEquals(200, code("PUT", "/admin/users/" + other, adminToken,
                Map.of("nickname", "other", "role", 0, "status", 1)));
        assertEquals(401, code("GET", "/admin/session", otherToken, null));
    }

    @Test
    void invalidEditsAndUnboundedPaginationAreRejected() throws Exception {
        assertEquals(400, code("PUT", "/article", ownerToken, Map.of("id", published, "title", "   ")));
        assertEquals(400, code("PUT", "/article", ownerToken, Map.of("id", published, "content", "   ")));
        assertEquals(400, code("GET", "/article/page?pageSize=-1", null, null));
        assertEquals(400, code("GET", "/article/page?pageNum=", null, null));
        assertEquals(400, code("GET", "/admin/users?pageNum=", adminToken, null));
        assertEquals(400, code("GET", "/article/admin/page?pageSize=101", adminToken, null));
        assertEquals(400, code("GET", "/article/mine/page?pageNum=0", ownerToken, null));
        assertEquals(400, code("PUT", "/admin/users/" + other, adminToken,
                Map.of("nickname", "other", "role", 0, "status", 1, "password", "      ")));
    }

    @Test
    void duplicateTaxonomyRenamesReturnConflict() throws Exception {
        long newCategory = insert("category", Map.of("name", prefix + "_created"));
        long newTag = insert("tag", Map.of("name", prefix + "_created"));
        assertEquals(409, code("PUT", "/category", adminToken, Map.of("id", newCategory, "name", prefix)));
        assertEquals(409, code("PUT", "/tag", adminToken, Map.of("id", newTag, "name", prefix)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"category", "tag"})
    void concurrentTaxonomyDeletionAndArticleCreationLeaveNoOrphans(String kind) throws Exception {
        long newCategory = insert("category", Map.of("name", prefix + "_created"));
        long newTag = insert("tag", Map.of("name", prefix + "_created"));
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Integer> creation = pool.submit(() -> { start.await(); return code("POST", "/article", ownerToken,
                    Map.of("title", prefix + "_concurrent", "content", "test", "categoryId", newCategory, "tagIds", List.of(newTag))); });
            Future<Integer> deletion = pool.submit(() -> { start.await(); return code("DELETE", "/" + kind + "/" + (kind.equals("category") ? newCategory : newTag), adminToken, null); });
            start.countDown();
            int created = creation.get(15, TimeUnit.SECONDS), deleted = deletion.get(15, TimeUnit.SECONDS);
            assertTrue((created == 200 && deleted == 409) || (created == 400 && deleted == 200), created + "/" + deleted);
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM article a LEFT JOIN category c ON c.id = a.category_id WHERE a.author_id = ? AND c.id IS NULL", Integer.class, owner));
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM article_tag at LEFT JOIN tag t ON t.id = at.tag_id WHERE at.article_id IN (SELECT id FROM article WHERE author_id = ?) AND t.id IS NULL", Integer.class, owner));
        } finally { pool.shutdownNow(); }
    }

    @Test
    void concurrentUserDeletionAndArticleCreationLeaveNoOrphans() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Integer> creation = pool.submit(() -> { start.await(); return code("POST", "/article", otherToken,
                    Map.of("title", prefix + "_other", "content", "test", "categoryId", category)); });
            Future<Integer> deletion = pool.submit(() -> { start.await(); return code("DELETE", "/admin/users/" + other, adminToken, null); });
            start.countDown();
            int created = creation.get(15, TimeUnit.SECONDS), deleted = deletion.get(15, TimeUnit.SECONDS);
            assertTrue((created == 200 && deleted == 409) || (created == 401 && deleted == 200), created + "/" + deleted);
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM article a LEFT JOIN user u ON u.id = a.author_id WHERE a.author_id = ? AND u.id IS NULL", Integer.class, other));
        } finally { pool.shutdownNow(); }
    }

    @Test
    void concurrentFollowAndUserDeletionLeaveNoOrphans() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Integer> follow = pool.submit(() -> { start.await(); return code("POST", "/user/follow/" + other, ownerToken, null); });
            Future<Integer> deletion = pool.submit(() -> { start.await(); return code("DELETE", "/admin/users/" + other, adminToken, null); });
            start.countDown();
            int followed = follow.get(15, TimeUnit.SECONDS), deleted = deletion.get(15, TimeUnit.SECONDS);
            assertTrue((followed == 200 && deleted == 409) || (followed == 404 && deleted == 200), followed + "/" + deleted);
            assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM user_follow f LEFT JOIN user u ON u.id = f.follow_user_id WHERE f.user_id = ? AND u.id IS NULL", Integer.class, owner));
        } finally { pool.shutdownNow(); }
    }

    @Test
    void concurrentMutualFollowsUseConsistentLockOrder() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Integer> first = pool.submit(() -> { start.await(); return code("POST", "/user/follow/" + other, ownerToken, null); });
            Future<Integer> second = pool.submit(() -> { start.await(); return code("POST", "/user/follow/" + owner, otherToken, null); });
            start.countDown();
            assertEquals(200, first.get(15, TimeUnit.SECONDS));
            assertEquals(200, second.get(15, TimeUnit.SECONDS));
            assertEquals(1, jdbc.queryForObject("SELECT following_count FROM user WHERE id = ?", Integer.class, owner));
            assertEquals(1, jdbc.queryForObject("SELECT follower_count FROM user WHERE id = ?", Integer.class, owner));
        } finally { pool.shutdownNow(); }
    }

    @Test
    void passwordChangeAndConcurrentOldPasswordLoginCannotLeaveValidOldSession() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<JsonNode> login = pool.submit(() -> { start.await(); return request("POST", "/user/login", null,
                    Map.of("username", prefix + "_other", "password", "testpass123")); });
            Future<Integer> update = pool.submit(() -> { start.await(); return code("PUT", "/admin/users/" + other, adminToken,
                    Map.of("nickname", "other", "role", 0, "status", 1, "password", "newpass123")); });
            start.countDown();
            assertEquals(200, update.get(15, TimeUnit.SECONDS));
            JsonNode result = login.get(15, TimeUnit.SECONDS);
            if (result.path("code").asInt() == 200) {
                String oldToken = result.path("data").asText(); tokens.add(oldToken);
                assertEquals(401, code("GET", "/user/me", oldToken, null));
            } else assertEquals(401, result.path("code").asInt());
            assertEquals(401, code("GET", "/user/me", otherToken, null));
            JsonNode newLogin = request("POST", "/user/login", null,
                    Map.of("username", prefix + "_other", "password", "newpass123"));
            assertEquals(200, newLogin.path("code").asInt());
            tokens.add(newLogin.path("data").asText());
        } finally { pool.shutdownNow(); }
    }

}
