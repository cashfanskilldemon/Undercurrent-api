-- 初始化数据：由 data/mock-data.json 生成
USE Undercurrent_test;

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE post_like; TRUNCATE TABLE post_save; TRUNCATE TABLE track_like;
TRUNCATE TABLE reply_like; TRUNCATE TABLE user_follow; TRUNCATE TABLE post_reply;
TRUNCATE TABLE post_tag; TRUNCATE TABLE post; TRUNCATE TABLE track_tag;
TRUNCATE TABLE track; TRUNCATE TABLE plugin; TRUNCATE TABLE app_user;
TRUNCATE TABLE genre; TRUNCATE TABLE channel; TRUNCATE TABLE plugin_category;
TRUNCATE TABLE tag_stat; TRUNCATE TABLE notification;
SET FOREIGN_KEY_CHECKS = 1;

-- 流派
INSERT INTO genre (id, name, sort_order) VALUES (1, 'Boom Bap', 1);
INSERT INTO genre (id, name, sort_order) VALUES (2, 'Trap', 2);
INSERT INTO genre (id, name, sort_order) VALUES (3, 'Phonk', 3);
INSERT INTO genre (id, name, sort_order) VALUES (4, 'Drill', 4);
INSERT INTO genre (id, name, sort_order) VALUES (5, 'UK Bass', 5);
INSERT INTO genre (id, name, sort_order) VALUES (6, 'Dub', 6);
INSERT INTO genre (id, name, sort_order) VALUES (7, 'Lo-fi', 7);
INSERT INTO genre (id, name, sort_order) VALUES (8, 'Breakcore', 8);

-- 讨论频道（"全部"是前端虚拟项，不入库）
INSERT INTO channel (id, name, slug, sort_order) VALUES (1, '作品反馈', 'feedback', 1);
INSERT INTO channel (id, name, slug, sort_order) VALUES (2, '混音母带', 'mixing', 2);
INSERT INTO channel (id, name, slug, sort_order) VALUES (3, '采样与版权', 'sampling', 3);
INSERT INTO channel (id, name, slug, sort_order) VALUES (4, '器材', 'gear', 4);
INSERT INTO channel (id, name, slug, sort_order) VALUES (5, '现场演出', 'live', 5);
INSERT INTO channel (id, name, slug, sort_order) VALUES (6, '闲聊', 'chat', 6);

-- 插件分类
INSERT INTO plugin_category (id, name, sort_order) VALUES (1, '合成器', 1);
INSERT INTO plugin_category (id, name, sort_order) VALUES (2, '效果器', 2);
INSERT INTO plugin_category (id, name, sort_order) VALUES (3, '鼓机', 3);
INSERT INTO plugin_category (id, name, sort_order) VALUES (4, '采样包', 4);
INSERT INTO plugin_category (id, name, sort_order) VALUES (5, '预设包', 5);

-- 用户（音乐人/制作人/插件作者/听众）
INSERT INTO app_user (id, handle, nickname, avatar_hue, city, role, followers_count, verified, created_at) VALUES (1, 'ghostwire', '阿鬼', 122, '成都', '音乐人', 18432, 1, NOW());
INSERT INTO app_user (id, handle, nickname, avatar_hue, city, role, followers_count, verified, created_at) VALUES (2, 'line9', '九号线', 42, '上海', '制作人', 9217, 1, NOW());
INSERT INTO app_user (id, handle, nickname, avatar_hue, city, role, followers_count, verified, created_at) VALUES (3, 'ashe.mp3', '灰烬', 15, '重庆', '音乐人', 26105, 0, NOW());
INSERT INTO app_user (id, handle, nickname, avatar_hue, city, role, followers_count, verified, created_at) VALUES (4, 'platformkid', '月台少年', 220, '北京', '制作人', 4388, 0, NOW());
INSERT INTO app_user (id, handle, nickname, avatar_hue, city, role, followers_count, verified, created_at) VALUES (5, 'lopressure', '低压槽', 160, '广州', '音乐人', 7741, 0, NOW());
INSERT INTO app_user (id, handle, nickname, avatar_hue, city, role, followers_count, verified, created_at) VALUES (6, 'nightshift404', '夜班404', 290, '杭州', '音乐人', 12960, 0, NOW());
INSERT INTO app_user (id, handle, nickname, avatar_hue, city, role, followers_count, verified, created_at) VALUES (7, 'solderdad', '老焊工', 70, '深圳', '插件作者', 3172, 1, NOW());
INSERT INTO app_user (id, handle, nickname, avatar_hue, city, role, followers_count, verified, created_at) VALUES (8, 'tapebin', '磁带回收站', 30, '西安', '插件作者', 5604, 0, NOW());
INSERT INTO app_user (id, handle, nickname, avatar_hue, city, role, followers_count, verified, created_at) VALUES (9, 'scallion', '小葱拌豆腐', 100, '长沙', '听众', 211, 0, NOW());
INSERT INTO app_user (id, handle, nickname, avatar_hue, city, role, followers_count, verified, created_at) VALUES (10, 'exit_two', 'DJ 二号口', 200, '武汉', '听众', 893, 0, NOW());

-- 管理员账号：admin / cashfans03（BCrypt cost 12）
INSERT INTO app_user (handle, nickname, avatar_hue, city, role, bio, followers_count, following_count, verified, password_hash, status) VALUES ('admin', '管理员', 122, '乌鲁木齐', '管理员', '站点管理员账号', 128, 12, 1, '$2a$12$3PIju6pvHQ2WVQA4Jh8YnexnyQsYw0HDh.AvKbcDVt8.50SGh76su', 1);

-- 作品
INSERT INTO track (id, artist_id, title, genre_id, bpm, musical_key, duration_sec, plays, likes_count, comments_count, cover, blurb, spec_json, released_at) VALUES (1, 1, '地铁末班', 1, 88, 'F 小调', 207, 48213, 3127, 214, 'cover-1.png', '录在二号线最后一班车上的环境声，鼓是 SP-404 里切的老唱片。', '{"bpm":88,"swing":0.16,"kick":"x.....x...x.....","snare":"....x.......x...","hat":"x.x.x.x.x.x.x.xx","bass":[0,null,null,null,null,null,-2,null,null,null,3,null,null,null,null,null],"root":41,"bassWave":"triangle","chord":[0,3,7,10]}', DATE_SUB(NOW(), INTERVAL 3 DAY));
INSERT INTO track (id, artist_id, title, genre_id, bpm, musical_key, duration_sec, plays, likes_count, comments_count, cover, blurb, spec_json, released_at) VALUES (2, 2, 'PAD 7', 5, 132, 'G 小调', 244, 21904, 1688, 97, 'cover-2.png', '一整首只用 MPC 第七个垫子的声音，做成了两步车库。', '{"bpm":132,"swing":0.22,"kick":"x.........x.....","snare":"....x.......x...","hat":"..x...x...x...xx","bass":[0,null,null,0,null,null,5,null,null,null,3,null,7,null,null,null],"root":43,"bassWave":"sawtooth"}', DATE_SUB(NOW(), INTERVAL 1 WEEK));
INSERT INTO track (id, artist_id, title, genre_id, bpm, musical_key, duration_sec, plays, likes_count, comments_count, cover, blurb, spec_json, released_at) VALUES (3, 3, '雾都漂移', 3, 140, 'C# 小调', 168, 96077, 8840, 532, 'cover-3.png', '牛铃换成了重庆轻轨报站声。808 开到失真，开车别听。', '{"bpm":140,"swing":0,"kick":"x.....x.x.......","snare":"....x.......x...","hat":"xxxxxxxxxxxxxxxx","bass":[0,null,null,null,null,null,0,null,3,null,null,null,null,null,-2,null],"root":37,"bassWave":"sine","long808":true}', DATE_SUB(NOW(), INTERVAL 2 WEEK));
INSERT INTO track (id, artist_id, title, genre_id, bpm, musical_key, duration_sec, plays, likes_count, comments_count, cover, blurb, spec_json, released_at) VALUES (4, 6, '十七层', 4, 142, 'A 小调', 191, 33580, 2401, 158, 'cover-4.png', '楼里只有一扇窗亮着，是我在剪 hi-hat。', '{"bpm":142,"swing":0,"kick":"x.......x.x.....","snare":"......x.......x.","hat":"x.xx.x.xx.x.xxx.","bass":[0,null,null,null,null,null,null,null,3,null,1,null,null,null,null,null],"root":33,"bassWave":"sine","long808":true}', DATE_SUB(NOW(), INTERVAL 5 DAY));
INSERT INTO track (id, artist_id, title, genre_id, bpm, musical_key, duration_sec, plays, likes_count, comments_count, cover, blurb, spec_json, released_at) VALUES (5, 5, '跳线打结', 6, 72, 'E 小调', 312, 12866, 1032, 61, 'cover-5.png', '模块合成器一条跳线没拔就录了，延迟反馈全是意外。', '{"bpm":72,"swing":0.08,"kick":"x.......x.......","snare":"........x.......","hat":"..x...x...x...x.","bass":[0,null,0,null,null,null,7,null,5,null,null,null,3,null,null,null],"root":40,"bassWave":"sine","chord":[0,7,10,15]}', DATE_SUB(NOW(), INTERVAL 3 WEEK));
INSERT INTO track (id, artist_id, title, genre_id, bpm, musical_key, duration_sec, plays, likes_count, comments_count, cover, blurb, spec_json, released_at) VALUES (6, 4, '天台磁带', 7, 82, 'D 大调', 156, 18350, 1977, 84, 'cover-6.png', '从二手市场淘的磁带，绞带那段我没剪掉。', '{"bpm":82,"swing":0.16,"kick":"x.....x...x.....","snare":"....x.......x...","hat":"x.x.x.x.x.x.x.xx","bass":[0,null,null,null,null,null,null,null,5,null,null,null,7,null,null,null],"root":38,"bassWave":"triangle","chord":[0,4,7,11]}', NOW());
INSERT INTO track (id, artist_id, title, genre_id, bpm, musical_key, duration_sec, plays, likes_count, comments_count, cover, blurb, spec_json, released_at) VALUES (7, 2, '断电重启', 8, 174, 'B 小调', 139, 7412, 640, 39, 'cover-2.png', 'Amen 切到第 400 刀的时候宿舍停电了。', '{"bpm":174,"swing":0,"kick":"x.x.......x.x...","snare":"....x..x.x..x..x","hat":"x.x.x.x.x.x.x.x.","bass":[0,null,null,null,null,null,null,null,-2,null,null,null,null,null,null,null],"root":35,"bassWave":"sawtooth"}', NOW());
INSERT INTO track (id, artist_id, title, genre_id, bpm, musical_key, duration_sec, plays, likes_count, comments_count, cover, blurb, spec_json, released_at) VALUES (8, 3, '火锅底料', 2, 136, 'D# 小调', 201, 61290, 5263, 377, 'cover-1.png', '和阿鬼在九眼桥边录的，副歌是凌晨三点写的。', '{"bpm":136,"swing":0,"kick":"x.....x...x.....","snare":"........x.......","hat":"x.x.xxx.x.x.x.xx","bass":[0,null,null,null,null,null,0,null,null,null,5,null,null,null,null,null],"root":39,"bassWave":"sine","long808":true}', NOW());

-- 作品标签
INSERT INTO track_tag (track_id, tag) VALUES (1, '采样');
INSERT INTO track_tag (track_id, tag) VALUES (1, 'SP-404');
INSERT INTO track_tag (track_id, tag) VALUES (1, '叙事');
INSERT INTO track_tag (track_id, tag) VALUES (2, '2-step');
INSERT INTO track_tag (track_id, tag) VALUES (2, 'MPC');
INSERT INTO track_tag (track_id, tag) VALUES (2, '极简');
INSERT INTO track_tag (track_id, tag) VALUES (3, '808');
INSERT INTO track_tag (track_id, tag) VALUES (3, '牛铃');
INSERT INTO track_tag (track_id, tag) VALUES (3, '失真');
INSERT INTO track_tag (track_id, tag) VALUES (4, '滑音 808');
INSERT INTO track_tag (track_id, tag) VALUES (4, '夜晚');
INSERT INTO track_tag (track_id, tag) VALUES (5, '模块');
INSERT INTO track_tag (track_id, tag) VALUES (5, '延迟');
INSERT INTO track_tag (track_id, tag) VALUES (5, '重低音');
INSERT INTO track_tag (track_id, tag) VALUES (6, '磁带');
INSERT INTO track_tag (track_id, tag) VALUES (6, '温暖');
INSERT INTO track_tag (track_id, tag) VALUES (6, '学习');
INSERT INTO track_tag (track_id, tag) VALUES (7, 'Amen');
INSERT INTO track_tag (track_id, tag) VALUES (7, '碎拍');
INSERT INTO track_tag (track_id, tag) VALUES (8, '合作');
INSERT INTO track_tag (track_id, tag) VALUES (8, '方言');

-- 插件
INSERT INTO plugin (id, maker_id, name, category_id, price, rating, reviews_count, downloads, formats, os, version, file_size, hue, knobs, tagline, description, updated_at) VALUES (1, 7, 'Soot 煤灰', 2, 0, 4.8, 412, 23871, 'VST3,AU,CLAP', 'Win,Mac', '1.4.2', '18.6 MB', 122, 4, '把干净的鼓变脏的磁带饱和器', '模拟三种磁带机的饱和与抖动，附带 wow / flutter 和磁头噪声。专门为 boom bap 的鼓组调教过，推到底也不会糊。', DATE_SUB(NOW(), INTERVAL 6 DAY));
INSERT INTO plugin (id, maker_id, name, category_id, price, rating, reviews_count, downloads, formats, os, version, file_size, hue, knobs, tagline, description, updated_at) VALUES (2, 7, 'Bolt 808', 3, 68, 4.6, 189, 6204, 'VST3,AU', 'Win,Mac', '2.0.0', '412 MB', 42, 6, '会自动滑音的 808 低音鼓', '内置 64 个从真机采样的 808，支持按音阶自动 glide、失真分频和侧链。Drill 和 phonk 制作人最常用的一块。', DATE_SUB(NOW(), INTERVAL 2 WEEK));
INSERT INTO plugin (id, maker_id, name, category_id, price, rating, reviews_count, downloads, formats, os, version, file_size, hue, knobs, tagline, description, updated_at) VALUES (3, 8, '旧货市场 Vol.3', 4, 39, 4.9, 267, 9318, 'WAV', 'Win,Mac', '3.0', '1.2 GB', 30, 0, '200 段已清版权的老唱片切片', '全部来自西安旧货市场淘到的黑胶和磁带，每一段都已经和版权方确认过可商用。按 BPM 和调性整理好了。', NOW());
INSERT INTO plugin (id, maker_id, name, category_id, price, rating, reviews_count, downloads, formats, os, version, file_size, hue, knobs, tagline, description, updated_at) VALUES (4, 7, 'Hum 嗡', 1, 128, 4.5, 93, 2716, 'VST3,AU,AAX', 'Win,Mac', '1.1.0', '96 MB', 200, 8, '专做低频的双振荡器合成器', '两个振荡器加一个只在 200Hz 以下工作的滤波器。没有花哨的预置，就是为了做 dub 和 UK bass 的 sub。', DATE_SUB(NOW(), INTERVAL 3 DAY));
INSERT INTO plugin (id, maker_id, name, category_id, price, rating, reviews_count, downloads, formats, os, version, file_size, hue, knobs, tagline, description, updated_at) VALUES (5, 8, 'Echo Chamber 回声室', 2, 0, 4.4, 158, 14092, 'VST3,AU', 'Win,Mac,Linux', '0.9.7', '7.4 MB', 160, 5, '模仿地下室混凝土墙的混响', '在真实地下室录的 12 组脉冲响应，附带一个能自激的弹簧延迟。开源，欢迎在讨论区提 bug。', NOW());
INSERT INTO plugin (id, maker_id, name, category_id, price, rating, reviews_count, downloads, formats, os, version, file_size, hue, knobs, tagline, description, updated_at) VALUES (6, 3, '雾都 Phonk 预设', 5, 25, 4.7, 341, 11507, 'Serum,Vital', 'Win,Mac', '1.0', '3.1 MB', 15, 3, '《雾都漂移》里用到的 48 个音色', '灰烬本人整理的牛铃、失真 808、和那条很脏的主音。Serum 和 Vital 双版本。', DATE_SUB(NOW(), INTERVAL 2 WEEK));
INSERT INTO plugin (id, maker_id, name, category_id, price, rating, reviews_count, downloads, formats, os, version, file_size, hue, knobs, tagline, description, updated_at) VALUES (7, 8, 'Chop Shop 切片铺', 2, 45, 4.3, 77, 3390, 'VST3,AU,CLAP', 'Win,Mac', '1.2.1', '22 MB', 290, 6, '实时切片和重排的 glitch 工具', '按拍子切片、倒放、跳帧，可以用 MIDI 触发。做 breakcore 和过门很顺手。', DATE_SUB(NOW(), INTERVAL 5 DAY));
INSERT INTO plugin (id, maker_id, name, category_id, price, rating, reviews_count, downloads, formats, os, version, file_size, hue, knobs, tagline, description, updated_at) VALUES (8, 5, '城中村 Field Recordings', 4, 0, 4.8, 122, 7651, 'WAV', 'Win,Mac,Linux', '1.0', '640 MB', 85, 0, '广州城中村 90 段环境录音', '早茶店、摩托车、楼道里的电视声。CC0 授权，随便用。', DATE_SUB(NOW(), INTERVAL 3 WEEK));

-- 帖子
INSERT INTO post (id, author_id, channel_id, body, likes_count, reposts_count, replies_count, pinned, attachment_kind, attachment_track_id, created_at) VALUES (1, 1, 1, '新歌《地铁末班》上线了。鼓全是从一张 80 年代的磁带里切的，低频我总觉得有点糊，有懂混音的兄弟帮听一下 0:42 那段吗？', 286, 41, 3, 1, 'track', 1, DATE_SUB(NOW(), INTERVAL 2 HOUR));
INSERT INTO post (id, author_id, channel_id, body, likes_count, reposts_count, replies_count, pinned, attachment_kind, attachment_track_id, created_at) VALUES (2, 3, 3, '说个可能得罪人的观点：地下圈子用采样不清版权，等歌火了再被下架，最后吃亏的还是自己。我现在宁愿花钱买清过的采样包。大家都怎么处理的？', 512, 97, 3, 0, NULL, NULL, DATE_SUB(NOW(), INTERVAL 4 HOUR));
INSERT INTO post (id, author_id, channel_id, body, likes_count, reposts_count, replies_count, pinned, attachment_kind, attachment_track_id, created_at) VALUES (3, 7, 4, 'Soot 1.4.2 更新了：修了 Ableton 12 里自动化跳变的问题，新加了一个「破旧随身听」磁头。免费的，插件区直接下。', 194, 58, 1, 0, 'plugin', NULL, DATE_SUB(NOW(), INTERVAL 6 HOUR));
INSERT INTO post (id, author_id, channel_id, body, likes_count, reposts_count, replies_count, pinned, attachment_kind, attachment_track_id, created_at) VALUES (4, 6, 5, '下周六杭州有一场地下室 showcase，五个本地 MC 加两个 beatmaker 现场切。场地只能塞 120 人，想来的在下面留城市，我统计一下要不要加场。', 341, 120, 2, 0, NULL, NULL, NOW());
INSERT INTO post (id, author_id, channel_id, body, likes_count, reposts_count, replies_count, pinned, attachment_kind, attachment_track_id, created_at) VALUES (5, 2, 2, 'UK bass 的 sub 到底该压到多少 LUFS？我把《PAD 7》推到 -8 之后在手机外放上完全听不到低音了，退回 -11 又觉得不够劲。', 127, 14, 1, 0, 'track', 2, NOW());
INSERT INTO post (id, author_id, channel_id, body, likes_count, reposts_count, replies_count, pinned, attachment_kind, attachment_track_id, created_at) VALUES (6, 4, 6, '今天在潘家园收了一箱磁带，老板说是一个 90 年代电台主持人的私藏。里面有好多没见过的本地乐队，准备慢慢数字化，有兴趣的可以一起听。', 89, 11, 0, 0, NULL, NULL, DATE_SUB(NOW(), INTERVAL 2 DAY));
INSERT INTO post (id, author_id, channel_id, body, likes_count, reposts_count, replies_count, pinned, attachment_kind, attachment_track_id, created_at) VALUES (7, 5, 1, '尝试了一首 72 BPM 的 dub，全程没有量化，延迟反馈手动拧的。这种不完美大家能接受吗，还是听起来就是不专业？', 76, 6, 1, 0, 'track', 5, DATE_SUB(NOW(), INTERVAL 3 DAY));

-- 帖子标签
INSERT INTO post_tag (thread_id, tag) VALUES (1, '求反馈');
INSERT INTO post_tag (thread_id, tag) VALUES (1, 'boombap');
INSERT INTO post_tag (thread_id, tag) VALUES (2, '版权');
INSERT INTO post_tag (thread_id, tag) VALUES (2, '采样');
INSERT INTO post_tag (thread_id, tag) VALUES (3, '插件更新');
INSERT INTO post_tag (thread_id, tag) VALUES (3, '免费');
INSERT INTO post_tag (thread_id, tag) VALUES (4, '演出');
INSERT INTO post_tag (thread_id, tag) VALUES (4, '杭州');
INSERT INTO post_tag (thread_id, tag) VALUES (5, '母带');
INSERT INTO post_tag (thread_id, tag) VALUES (5, 'LUFS');
INSERT INTO post_tag (thread_id, tag) VALUES (6, '磁带');
INSERT INTO post_tag (thread_id, tag) VALUES (6, '挖碟');
INSERT INTO post_tag (thread_id, tag) VALUES (7, 'dub');
INSERT INTO post_tag (thread_id, tag) VALUES (7, '求反馈');

-- 回复
INSERT INTO post_reply (id, thread_id, author_id, body, likes_count, created_at) VALUES (1, 1, 5, '0:42 底鼓和贝斯在 60Hz 打架了，试试给贝斯做一个跟底鼓的侧链，或者底鼓往 50Hz 挪一点。', 48, DATE_SUB(NOW(), INTERVAL 1 HOUR));
INSERT INTO post_reply (id, thread_id, author_id, body, likes_count, created_at) VALUES (2, 1, 4, '磁带的那层嘶声太好听了，别降噪。', 31, DATE_SUB(NOW(), INTERVAL 58 MINUTE));
INSERT INTO post_reply (id, thread_id, author_id, body, likes_count, created_at) VALUES (3, 1, 9, '通勤路上循环第五遍了', 12, DATE_SUB(NOW(), INTERVAL 22 MINUTE));
INSERT INTO post_reply (id, thread_id, author_id, body, likes_count, created_at) VALUES (4, 2, 8, '同意。我们做《旧货市场》系列就是因为被下架过两次，一段段去找版权方谈的。', 88, DATE_SUB(NOW(), INTERVAL 3 HOUR));
INSERT INTO post_reply (id, thread_id, author_id, body, likes_count, created_at) VALUES (5, 2, 2, '小样随便采，正式发行前一定要清。这是底线。', 64, DATE_SUB(NOW(), INTERVAL 3 HOUR));
INSERT INTO post_reply (id, thread_id, author_id, body, likes_count, created_at) VALUES (6, 2, 10, '能不能出一个版权清理的教程帖，置顶那种', 27, DATE_SUB(NOW(), INTERVAL 2 HOUR));
INSERT INTO post_reply (id, thread_id, author_id, body, likes_count, created_at) VALUES (7, 3, 1, '老焊工yyds，新磁头放在鼓总线上直接出味道。', 22, DATE_SUB(NOW(), INTERVAL 5 HOUR));
INSERT INTO post_reply (id, thread_id, author_id, body, likes_count, created_at) VALUES (8, 4, 10, '武汉，坐高铁过去！', 9, NOW());
INSERT INTO post_reply (id, thread_id, author_id, body, likes_count, created_at) VALUES (9, 4, 9, '长沙 +1，求加场', 6, NOW());
INSERT INTO post_reply (id, thread_id, author_id, body, likes_count, created_at) VALUES (10, 5, 5, '手机外放本来就放不出 60Hz 以下，加一点 120Hz 左右的谐波失真让耳朵「脑补」出低音。', 53, NOW());
INSERT INTO post_reply (id, thread_id, author_id, body, likes_count, created_at) VALUES (11, 7, 1, '地下要什么专业，这就是味道。', 19, DATE_SUB(NOW(), INTERVAL 3 DAY));

-- 标签热度
INSERT INTO tag_stat (tag, posts) VALUES ('求反馈', 1284);
INSERT INTO tag_stat (tag, posts) VALUES ('boombap', 962);
INSERT INTO tag_stat (tag, posts) VALUES ('版权', 517);
INSERT INTO tag_stat (tag, posts) VALUES ('808', 488);
INSERT INTO tag_stat (tag, posts) VALUES ('插件更新', 301);
INSERT INTO tag_stat (tag, posts) VALUES ('演出', 276);
INSERT INTO tag_stat (tag, posts) VALUES ('磁带', 193);

-- 站内通知（6 条，接收者=user 2）
INSERT INTO notification (recipient_id, actor_id, type, body, href, is_read, created_at) VALUES (2, 5, 'like', '低压槽 赞了你在「地铁末班」下的回复', '/threads/1', 0, DATE_SUB(NOW(), INTERVAL 12 MINUTE));
INSERT INTO notification (recipient_id, actor_id, type, body, href, is_read, created_at) VALUES (2, 8, 'reply', '磁带回收站 回复了你：采样清版权这事，早点做完比事后补救便宜', '/threads/2', 0, DATE_SUB(NOW(), INTERVAL 48 MINUTE));
INSERT INTO notification (recipient_id, actor_id, type, body, href, is_read, created_at) VALUES (2, 3, 'follow', '灰烬 关注了你', '/u/ashe.mp3', 0, DATE_SUB(NOW(), INTERVAL 2 HOUR));
INSERT INTO notification (recipient_id, actor_id, type, body, href, is_read, created_at) VALUES (2, NULL, 'system', '本周地下榜更新：8 首新作品进入榜单', '/tracks', 0, DATE_SUB(NOW(), INTERVAL 5 HOUR));
INSERT INTO notification (recipient_id, actor_id, type, body, href, is_read, created_at) VALUES (2, 1, 'like', '阿鬼 赞了你的帖子', '/threads', 0, DATE_SUB(NOW(), INTERVAL 1 DAY));
INSERT INTO notification (recipient_id, actor_id, type, body, href, is_read, created_at) VALUES (2, NULL, 'system', 'Soot 1.4.2 已发布，插件区可直接下载', '/plugins', 0, DATE_SUB(NOW(), INTERVAL 2 DAY));

-- 关注关系：用户 2 关注 1/3/5/9，供首页关注流与发现页使用
INSERT INTO user_follow (follower_id, followee_id) VALUES (2, 1);
INSERT INTO user_follow (follower_id, followee_id) VALUES (2, 3);
INSERT INTO user_follow (follower_id, followee_id) VALUES (2, 5);
INSERT INTO user_follow (follower_id, followee_id) VALUES (2, 9);
UPDATE app_user SET following_count = 4 WHERE id = 2;
UPDATE app_user SET followers_count = followers_count + 1 WHERE id IN (1, 3, 5, 9);
