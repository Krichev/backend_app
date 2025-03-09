-- Sample Data for H2 Database

-- Insert Users
INSERT INTO users (username, email, password, profile_picture_url, bio, created_at, updated_at)
VALUES
    ('john_fitness', 'john@example.com', '$2a$10$XLqAH.vKzIY9JbDQ9G7w.eTNOE69wx4L4XzQZ9t7nSD5rk2S3VhJa', 'https://profiles/john.jpg', 'Fitness enthusiast, love running and hiking', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('alice_runner', 'alice@example.com', '$2a$10$XLqAH.vKzIY9JbDQ9G7w.eTNOE69wx4L4XzQZ9t7nSD5rk2S3VhJa', 'https://profiles/alice.jpg', 'Marathon runner, setting new personal records every month', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('bob_quiz', 'bob@example.com', '$2a$10$XLqAH.vKzIY9JbDQ9G7w.eTNOE69wx4L4XzQZ9t7nSD5rk2S3VhJa', 'https://profiles/bob.jpg', 'Trivia master, love creating and solving quizzes', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('emma_social', 'emma@example.com', '$2a$10$XLqAH.vKzIY9JbDQ9G7w.eTNOE69wx4L4XzQZ9t7nSD5rk2S3VhJa', 'https://profiles/emma.jpg', 'Social butterfly looking for activity partners', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('david_skater', 'david@example.com', '$2a$10$XLqAH.vKzIY9JbDQ9G7w.eTNOE69wx4L4XzQZ9t7nSD5rk2S3VhJa', 'https://profiles/david.jpg', 'Ice skating enthusiast, looking for skating buddies', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert Groups
INSERT INTO groups (name, description, type, privacy_setting, creator_id, created_at, updated_at)
VALUES
    ('Morning Runners', 'Group for people who love to run in the morning', 'CHALLENGE', 'PUBLIC', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Trivia Geeks', 'A group for trivia enthusiasts', 'SOCIAL', 'PUBLIC', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Fitness Accountability', 'Hold each other accountable for fitness goals', 'CHALLENGE', 'PRIVATE', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Skating Club', 'Find skating partners and events', 'SOCIAL', 'PUBLIC', 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Weight Loss Challenge', 'Support group for weight loss goals', 'CHALLENGE', 'INVITATION_ONLY', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert Group Members
INSERT INTO group_users (group_id, user_id, role, join_date)
VALUES
    (1, 1, 'MEMBER', CURRENT_TIMESTAMP),
    (1, 2, 'ADMIN', CURRENT_TIMESTAMP),
    (1, 4, 'MEMBER', CURRENT_TIMESTAMP),
    (2, 3, 'ADMIN', CURRENT_TIMESTAMP),
    (2, 4, 'MEMBER', CURRENT_TIMESTAMP),
    (2, 5, 'MEMBER', CURRENT_TIMESTAMP),
    (3, 1, 'ADMIN', CURRENT_TIMESTAMP),
    (3, 2, 'MEMBER', CURRENT_TIMESTAMP),
    (3, 5, 'MEMBER', CURRENT_TIMESTAMP),
    (4, 4, 'MEMBER', CURRENT_TIMESTAMP),
    (4, 5, 'ADMIN', CURRENT_TIMESTAMP),
    (5, 1, 'ADMIN', CURRENT_TIMESTAMP),
    (5, 2, 'MODERATOR', CURRENT_TIMESTAMP),
    (5, 4, 'MEMBER', CURRENT_TIMESTAMP);

-- Insert User Connections
INSERT INTO user_connections (user_id, connected_user_id, status, created_at, updated_at)
VALUES
    (1, 2, 'ACCEPTED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, 3, 'ACCEPTED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (1, 4, 'PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 3, 'ACCEPTED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 5, 'ACCEPTED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 5, 'PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (4, 5, 'ACCEPTED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert Quests
INSERT INTO quests (title, description, type, visibility, status, creator_id, created_at, updated_at)
VALUES
    ('Daily Run Challenge', 'Run at least 1 mile every day for a month', 'CHALLENGE', 'GROUP_ONLY', 'OPEN', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Movie Trivia Night', 'Test your knowledge of classic movies', 'QUIZ', 'PUBLIC', 'OPEN', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('30-Day Fitness Challenge', 'Complete daily exercises for 30 days', 'CHALLENGE', 'GROUP_ONLY', 'IN_PROGRESS', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Skating Partner Needed', 'Looking for someone to go ice skating with this weekend', 'ACTIVITY_PARTNER', 'PUBLIC', 'OPEN', 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Walking Buddies', 'Need a walking partner for daily morning walks', 'ACTIVITY_PARTNER', 'PUBLIC', 'OPEN', 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Link Quests to Groups
INSERT INTO quest_groups (quest_id, group_id)
VALUES
    (1, 1),
    (2, 2),
    (3, 3),
    (3, 5),
    (4, 4);

-- Link Users to Quests
INSERT INTO user_quests (user_id, quest_id, status, join_date)
VALUES
    (1, 1, 'ACTIVE', CURRENT_TIMESTAMP),
    (2, 1, 'ACTIVE', CURRENT_TIMESTAMP),
    (3, 2, 'ACTIVE', CURRENT_TIMESTAMP),
    (4, 2, 'ACTIVE', CURRENT_TIMESTAMP),
    (5, 2, 'ACTIVE', CURRENT_TIMESTAMP),
    (1, 3, 'ACTIVE', CURRENT_TIMESTAMP),
    (2, 3, 'ACTIVE', CURRENT_TIMESTAMP),
    (5, 3, 'ACTIVE', CURRENT_TIMESTAMP),
    (4, 4, 'ACTIVE', CURRENT_TIMESTAMP),
    (4, 5, 'ACTIVE', CURRENT_TIMESTAMP);

-- Insert Tasks
INSERT INTO tasks (title, description, type, status, verification_method, start_date, end_date, quest_id, assigned_to, created_at, updated_at)
VALUES
    ('Run 1 mile', 'Complete a 1-mile run and track with fitness app', 'DAILY', 'IN_PROGRESS', 'FITNESS_API', CURRENT_TIMESTAMP, DATEADD(MONTH, 1, CURRENT_TIMESTAMP), 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Movie Characters Quiz', 'Identify 10 famous movie characters', 'ONE_TIME', 'NOT_STARTED', 'QUIZ', CURRENT_TIMESTAMP, DATEADD(DAY, 7, CURRENT_TIMESTAMP), 2, 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Daily Push-ups', 'Complete 20 push-ups daily', 'DAILY', 'IN_PROGRESS', 'PHOTO', CURRENT_TIMESTAMP, DATEADD(DAY, 30, CURRENT_TIMESTAMP), 3, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Weekend Skating', 'Go ice skating this weekend', 'ONE_TIME', 'NOT_STARTED', 'PHOTO', CURRENT_TIMESTAMP, DATEADD(DAY, 5, CURRENT_TIMESTAMP), 4, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Morning Walk', 'Walk for 30 minutes in the morning', 'DAILY', 'NOT_STARTED', 'FITNESS_API', CURRENT_TIMESTAMP, DATEADD(DAY, 14, CURRENT_TIMESTAMP), 5, 4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert Task Completions
INSERT INTO task_completions (task_id, user_id, status, completion_date, verification_date, verification_proof, notes, created_at)
VALUES
    (1, 1, 'VERIFIED', DATEADD(DAY, -1, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP, 'https://api.fitness.com/proof/123', 'Completed 1.2 miles', CURRENT_TIMESTAMP),
    (3, 1, 'SUBMITTED', CURRENT_TIMESTAMP, NULL, 'https://photos.app/pushups/123', 'Completed 25 push-ups', CURRENT_TIMESTAMP);

-- Insert Rewards
INSERT INTO rewards (description, type, monetary_value, currency, reward_source, quest_id, created_at)
VALUES
    ('Daily Runner Reward', 'MONETARY', 5.00, 'USD', 'GROUP', 1, CURRENT_TIMESTAMP),
    ('Trivia Winner Badge', 'BADGE', NULL, NULL, 'SYSTEM', 2, CURRENT_TIMESTAMP),
    ('Fitness Achievement', 'POINTS', 100.00, 'POINTS', 'GROUP', 3, CURRENT_TIMESTAMP),
    ('Skating Partner Finder', 'BADGE', NULL, NULL, 'SYSTEM', 4, CURRENT_TIMESTAMP),
    ('Fitness Commitment Cash', 'MONETARY', 10.00, 'USD', 'INDIVIDUAL', 3, CURRENT_TIMESTAMP);

-- Link Rewards to Users
INSERT INTO reward_users (reward_id, user_id, awarded_date)
VALUES
    (1, 1, DATEADD(DAY, -1, CURRENT_TIMESTAMP)),
    (2, 3, DATEADD(DAY, -3, CURRENT_TIMESTAMP)),
    (2, 4, DATEADD(DAY, -3, CURRENT_TIMESTAMP));

-- Insert User Activity Logs
INSERT INTO user_activity_logs (user_id, activity_type, description, created_at)
VALUES
    (1, 'TASK_COMPLETION', 'Completed daily running task', DATEADD(DAY, -1, CURRENT_TIMESTAMP)),
    (1, 'JOIN_GROUP', 'Joined Morning Runners group', DATEADD(DAY, -1, CURRENT_TIMESTAMP))