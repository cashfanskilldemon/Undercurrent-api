package com.undercurrent.service;

import com.undercurrent.common.BusinessException;
import com.undercurrent.domain.User;
import com.undercurrent.domain.UserLink;
import com.undercurrent.dto.UpdateProfileRequest;
import com.undercurrent.dto.UserLinkRequest;
import com.undercurrent.mapper.UserLinkMapper;
import com.undercurrent.mapper.UserMapper;
import com.undercurrent.vo.UserLinkVO;
import com.undercurrent.vo.UserProfileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final UserLinkMapper userLinkMapper;

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    private static final long MAX_AVATAR_BYTES = 8L * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    public List<UserProfileVO> suggested(Long viewerId, String keyword, int limit) {
        return userMapper.selectSuggested(viewerId, keyword, Math.min(50, Math.max(1, limit)));
    }

    public UserProfileVO detail(Long id, Long viewerId) {
        UserProfileVO vo = userMapper.selectProfile(id, viewerId);
        if (vo == null) {
            throw BusinessException.notFound("用户");
        }
        return withLinks(vo);
    }

    public UserProfileVO byHandle(String handle, Long viewerId) {
        UserProfileVO vo = userMapper.selectByHandle(handle, viewerId);
        if (vo == null) {
            throw BusinessException.notFound("用户");
        }
        return withLinks(vo);
    }

    /** 编辑自己的资料：只能改自己，昵称/简介/城市做长度与空白约束 */
    @Transactional
    public UserProfileVO updateProfile(Long id, UpdateProfileRequest req, Long viewerId) {
        if (viewerId == null) {
            throw new BusinessException(401, "请先登录");
        }
        if (!viewerId.equals(id)) {
            throw new BusinessException(403, "只能编辑自己的资料");
        }
        UserProfileVO current = userMapper.selectProfile(id, viewerId);
        if (current == null) {
            throw BusinessException.notFound("用户");
        }
        String nickname = trimToNull(req.getNickname());
        if (nickname != null && nickname.length() > 40) {
            throw new BusinessException(400, "昵称最多 40 字");
        }
        String bio = trimToNull(req.getBio());
        if (bio != null && bio.length() > 200) {
            throw new BusinessException(400, "简介最多 200 字");
        }
        String city = trimToNull(req.getCity());
        if (city != null && city.length() > 40) {
            throw new BusinessException(400, "城市最多 40 字");
        }
        // 名字限制：14 天内最多改两次（参照 Threads / Instagram）
        boolean nameChanged = nickname != null && !nickname.equals(current.getName());
        if (nameChanged) {
            int used = userMapper.countNameChangesSince(id, java.time.LocalDateTime.now().minusDays(14));
            if (used >= 2) {
                throw new BusinessException(400, "14 天内只能更改名字两次");
            }
        }
        if (nickname != null) {
            userMapper.updateProfile(id, nickname, bio, city);
            if (nameChanged) {
                userMapper.insertNameChange(id);
            }
        }
        return withLinks(userMapper.selectProfile(id, viewerId));
    }

    /** 上传头像：base64 解码存 {userId}.img，覆盖旧图；返回带时间戳的路径防缓存 */
    @Transactional
    public UserProfileVO uploadAvatar(Long id, com.undercurrent.dto.AvatarUploadRequest req, Long viewerId) {
        assertSelf(id, viewerId);
        if (req == null || req.getData() == null || req.getData().isBlank()) {
            throw new BusinessException(400, "请选择图片");
        }
        String data = req.getData();
        // 兼容 data:image/png;base64,xxx 前缀
        int comma = data.indexOf(',');
        if (data.startsWith("data:") && comma > 0) {
            // data:image/jpeg;base64,xxx —— mime 在 ; 之前，; 后是编码声明
            String head = data.substring(5, comma).toLowerCase();
            int semi = head.indexOf(';');
            String mime = semi >= 0 ? head.substring(0, semi) : head;
            if (!ALLOWED_TYPES.contains(mime)) {
                throw new BusinessException(400, "只支持 JPG / PNG / WebP / GIF");
            }
            data = data.substring(comma + 1);
        }
        byte[] bytes;
        try {
            bytes = java.util.Base64.getDecoder().decode(data);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(400, "图片数据格式错误");
        }
        if (bytes.length == 0 || bytes.length > MAX_AVATAR_BYTES) {
            throw new BusinessException(400, "头像不能超过 8MB");
        }
        // 验图：用 ImageIO 确认是真图片，顺带统一转成 PNG 落盘
        java.awt.image.BufferedImage image;
        try {
            image = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(bytes));
        } catch (IOException e) {
            image = null;
        }
        if (image == null) {
            throw new BusinessException(400, "图片无法识别");
        }
        // 头像显示尺寸最大 86px，长边统一缩到 512 落盘（小图不放大），省磁盘、加载快
        int w = image.getWidth();
        int h = image.getHeight();
        int maxSide = Math.max(w, h);
        if (maxSide > 512) {
            double scale = 512.0 / maxSide;
            int tw = Math.max(1, (int) Math.round(w * scale));
            int th = Math.max(1, (int) Math.round(h * scale));
            java.awt.Image scaled = image.getScaledInstance(tw, th, java.awt.Image.SCALE_SMOOTH);
            java.awt.image.BufferedImage out = new java.awt.image.BufferedImage(tw, th, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g = out.createGraphics();
            g.drawImage(scaled, 0, 0, null);
            g.dispose();
            image = out;
        }
        try {
            Path dir = Paths.get(uploadDir, "avatars");
            Files.createDirectories(dir);
            Path target = dir.resolve(id + ".png");
            javax.imageio.ImageIO.write(image, "png", target.toFile());
        } catch (IOException e) {
            throw new BusinessException(500, "头像保存失败: " + e.getMessage());
        }
        String avatar = "/avatars/" + id + ".png?t=" + System.currentTimeMillis();
        userMapper.updateAvatar(id, avatar);
        return withLinks(userMapper.selectProfile(id, viewerId));
    }

    /** 移除头像：删文件并清空数据库路径，回退到色块占位 */
    @Transactional
    public UserProfileVO removeAvatar(Long id, Long viewerId) {
        assertSelf(id, viewerId);
        try {
            Files.deleteIfExists(Paths.get(uploadDir, "avatars", id + ".png"));
            Files.deleteIfExists(Paths.get(uploadDir, "avatars", id + ".jpg"));
        } catch (IOException ignored) {
            // 文件已不存在也算移除成功
        }
        userMapper.updateAvatar(id, null);
        return withLinks(userMapper.selectProfile(id, viewerId));
    }

    /** 个人主页链接列表 */
    public List<UserLinkVO> listLinks(Long id, Long viewerId) {
        if (viewerId == null) {
            throw new BusinessException(401, "请先登录");
        }
        if (!viewerId.equals(id)) {
            throw new BusinessException(403, "只能查看自己的链接");
        }
        return userLinkMapper.selectByUser(id);
    }

    /** 添加主页链接，最多 8 条 */
    @Transactional
    public UserLinkVO addLink(Long id, UserLinkRequest req, Long viewerId) {
        assertSelf(id, viewerId);
        if (req == null) {
            throw new BusinessException(400, "请填写链接");
        }
        if (userLinkMapper.countByUser(id) >= 8) {
            throw new BusinessException(400, "最多添加 8 条链接");
        }
        String url = normalizeUrl(req.getUrl());
        String title = trimToNull(req.getTitle());
        if (title != null && title.length() > 40) {
            throw new BusinessException(400, "标题最多 40 字");
        }
        UserLink link = new UserLink();
        link.setUserId(id);
        link.setUrl(url);
        link.setTitle(title);
        link.setSortOrder(userLinkMapper.countByUser(id));
        userLinkMapper.insert(link);
        return userLinkMapper.selectByUser(id).stream()
                .filter(l -> l.getId().equals(link.getId()))
                .findFirst()
                .orElse(null);
    }

    /** 删除自己的主页链接 */
    @Transactional
    public void removeLink(Long id, Long linkId, Long viewerId) {
        assertSelf(id, viewerId);
        if (userLinkMapper.deleteById(linkId, id) == 0) {
            throw BusinessException.notFound("链接");
        }
    }

    private UserProfileVO withLinks(UserProfileVO vo) {
        if (vo != null) {
            vo.setLinks(userLinkMapper.selectByUser(vo.getId()));
        }
        return vo;
    }

    private String normalizeUrl(String raw) {
        String url = trimToNull(raw);
        if (url == null) {
            throw new BusinessException(400, "请输入网址");
        }
        if (!url.matches("(?i)^https?://.+")) {
            url = "https://" + url;
        }
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host == null || host.isBlank()
                    || (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme()))) {
                throw new BusinessException(400, "网址格式不正确");
            }
        } catch (URISyntaxException e) {
            throw new BusinessException(400, "网址格式不正确");
        }
        if (url.length() > 500) {
            throw new BusinessException(400, "网址太长");
        }
        return url;
    }

    private void assertSelf(Long id, Long viewerId) {
        if (viewerId == null) {
            throw new BusinessException(401, "请先登录");
        }
        if (!viewerId.equals(id)) {
            throw new BusinessException(403, "只能编辑自己的资料");
        }
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /** 关注切换，返回最新关注态与粉丝数 */
    @Transactional
    public Map<String, Object> toggleFollow(Long targetId, Long viewerId) {
        if (viewerId == null) {
            throw new BusinessException(401, "请先登录");
        }
        if (targetId == null || userMapper.selectProfile(targetId, viewerId) == null) {
            throw BusinessException.notFound("用户");
        }
        if (targetId.equals(viewerId)) {
            throw new BusinessException(400, "不能关注自己");
        }
        boolean following = userMapper.existsFollow(viewerId, targetId) > 0;
        if (following) {
            userMapper.deleteFollow(viewerId, targetId);
            userMapper.addFollowerCount(targetId, -1);
            userMapper.addFollowingCount(viewerId, -1);
        } else {
            userMapper.insertFollow(viewerId, targetId);
            userMapper.addFollowerCount(targetId, 1);
            userMapper.addFollowingCount(viewerId, 1);
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("followed", !following);
        UserProfileVO fresh = userMapper.selectProfile(targetId, viewerId);
        r.put("followers", fresh != null ? fresh.getFollowers() : 0);
        return r;
    }

    public List<Long> followeeIds(Long viewerId) {
        if (viewerId == null) {
            return List.of();
        }
        return userMapper.selectFolloweeIds(viewerId);
    }

    /** 新注册用户 / 没走过引导的老用户返回 true，前端据此先跳 /onboarding */
    public boolean needsOnboarding(Long userId) {
        if (userId == null) {
            return false;
        }
        User user = userMapper.selectOnboardingState(userId);
        return user == null || user.getOnboardedAt() == null;
    }
}
