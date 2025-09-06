-- Seed data for Challenger Application
-- ======================================

-- Insert sample users
INSERT INTO users (username, email, password, bio, role) VALUES
                                                             ('admin', 'admin@challenger.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye83B6c4U9K7Q4g4XuQ0xL0J6TbZ8RvfG', 'System Administrator', 'ADMIN'),
                                                             ('john_doe', 'john@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye83B6c4U9K7Q4g4XuQ0xL0J6TbZ8RvfG', 'Fitness enthusiast and challenge creator', 'MEMBER'),
                                                             ('jane_smith', 'jane@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye83B6c4U9K7Q4g4XuQ0xL0J6TbZ8RvfG', 'Love learning new things through challenges', 'MEMBER'),
                                                             ('mike_wilson', 'mike@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye83B6c4U9K7Q4g4XuQ0xL0J6TbZ8RvfG', 'Community moderator', 'MODERATOR'),
                                                             ('sarah_connor', 'sarah@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMye83B6c4U9K7Q4g4XuQ0xL0J6TbZ8RvfG', 'Always up for a challenge!', 'MEMBER');

-- Insert sample groups
INSERT INTO groups (name, description, type, privacy_setting, creator_id) VALUES
                                                                              ('Fitness Warriors', 'A group for fitness enthusiasts who love challenges', 'FITNESS', 'PUBLIC', 2),
                                                                              ('Learning Together', 'Community for educational challenges and skill building', 'LEARNING', 'PUBLIC', 3),
                                                                              ('Social Butterflies', 'Fun social challenges and activities', 'SOCIAL', 'PUBLIC', 4),
                                                                              ('Weekend Adventurers', 'Outdoor and adventure challenges', 'HOBBY', 'INVITATION_ONLY', 5);

-- Insert group memberships
INSERT INTO group_users (group_id, user_id) VALUES
                                                (1, 2), (1, 3), (1, 4),  -- Fitness Warriors members
                                                (2, 1), (2, 3), (2, 5),  -- Learning Together members
                                                (3, 2), (3, 4), (3, 5),  -- Social Butterflies members
                                                (4, 2), (4, 3), (4, 5);  -- Weekend Adventurers members

-- Insert sample challenges
INSERT INTO challenges (title, description, type, creator_id, group_id, start_date, end_date, frequency, status) VALUES
                                                                                                                     ('30-Day Fitness Challenge', 'Complete daily workouts for 30 days', 'ACCOUNTABILITY', 2, 1,
                                                                                                                      CURRENT_DATE, CURRENT_DATE + INTERVAL '30 days', 'DAILY', 'ACTIVE'),
                                                                                                                     ('Learn a New Language', 'Practice Spanish for 15 minutes daily', 'QUEST', 3, 2,
                                                                                                                      CURRENT_DATE, CURRENT_DATE + INTERVAL '60 days', 'DAILY', 'ACTIVE'),
                                                                                                                     ('Weekend Hiking Challenge', 'Complete 4 hikes this month', 'EVENT', 5, 4,
                                                                                                                      CURRENT_DATE, CURRENT_DATE + INTERVAL '30 days', 'WEEKLY', 'ACTIVE'),
                                                                                                                     ('Reading Marathon', 'Read 12 books in 3 months', 'QUEST', 1, 2,
                                                                                                                      CURRENT_DATE, CURRENT_DATE + INTERVAL '90 days', 'WEEKLY', 'PENDING');

-- Insert challenge participants
INSERT INTO challenge_participants (challenge_id, user_id) VALUES
                                                               (1, 2), (1, 3), (1, 4),  -- 30-Day Fitness Challenge
                                                               (2, 3), (2, 5),          -- Learn a New Language
                                                               (3, 2), (3, 5),          -- Weekend Hiking Challenge
                                                               (4, 1), (4, 3), (4, 4);  -- Reading Marathon

-- Insert challenge progress
INSERT INTO challenge_progress (challenge_id, user_id, status, completion_percentage) VALUES
                                                                                          (1, 2, 'IN_PROGRESS', 25.5),
                                                                                          (1, 3, 'IN_PROGRESS', 18.2),
                                                                                          (1, 4, 'IN_PROGRESS', 35.0),
                                                                                          (2, 3, 'IN_PROGRESS', 40.0),
                                                                                          (2, 5, 'IN_PROGRESS', 22.5),
                                                                                          (3, 2, 'IN_PROGRESS', 50.0),
                                                                                          (3, 5, 'IN_PROGRESS', 75.0);

-- Insert some completed days for daily challenges
INSERT INTO challenge_progress_completed_days (challenge_progress_id, completed_day) VALUES
                                                                                         (1, CURRENT_DATE - INTERVAL '1 day'),
                                                                                         (1, CURRENT_DATE - INTERVAL '2 days'),
                                                                                         (1, CURRENT_DATE - INTERVAL '3 days'),
                                                                                         (2, CURRENT_DATE - INTERVAL '1 day'),
                                                                                         (2, CURRENT_DATE - INTERVAL '3 days'),
                                                                                         (3, CURRENT_DATE - INTERVAL '1 day'),
                                                                                         (3, CURRENT_DATE - INTERVAL '2 days'),
                                                                                         (3, CURRENT_DATE - INTERVAL '3 days'),
                                                                                         (3, CURRENT_DATE - INTERVAL '4 days');

-- Insert sample quests
INSERT INTO quests (title, description, type, visibility, status, creator_id, challenge_id) VALUES
                                                                                                ('Morning Workout', 'Complete a 30-minute morning workout', 'CHALLENGE', 'PUBLIC', 'OPEN', 2, 1),
                                                                                                ('Vocabulary Practice', 'Learn 10 new Spanish words', 'LEARNING', 'PUBLIC', 'OPEN', 3, 2),
                                                                                                ('Mountain Trail Hike', 'Complete the Blue Ridge trail', 'CHALLENGE', 'PUBLIC', 'OPEN', 5, 3),
                                                                                                ('Book Club Discussion', 'Read and discuss "The Alchemist"', 'LEARNING', 'GROUP_ONLY', 'OPEN', 1, 4);

-- Insert sample tasks
INSERT INTO tasks (title, description, type, status, verification_method, quest_id, assigned_to, challenge_id) VALUES
                                                                                                                   ('Push-ups', 'Do 50 push-ups', 'DAILY', 'NOT_STARTED', 'MANUAL', 1, 2, 1),
                                                                                                                   ('Spanish Flashcards', 'Review vocabulary flashcards', 'DAILY', 'NOT_STARTED', 'MANUAL', 2, 3, 2),
                                                                                                                   ('Trail Preparation', 'Pack hiking gear and supplies', 'ONE_TIME', 'NOT_STARTED', 'PHOTO', 3, 5, 3),
                                                                                                                   ('Chapter 1 Summary', 'Write summary of first chapter', 'ONE_TIME', 'NOT_STARTED', 'MANUAL', 4, 1, 4);

-- Insert sample rewards
INSERT INTO rewards (title, description, type, value, quest_id) VALUES
                                                                    ('Fitness Badge', 'Awarded for completing daily workouts', 'BADGE', 0, 1),
                                                                    ('Language Learner', 'Recognition for vocabulary mastery', 'BADGE', 0, 2),
                                                                    ('Trail Blazer', 'Achievement for completing challenging hikes', 'BADGE', 0, 3),
                                                                    ('Bookworm Badge', 'Awarded for reading achievements', 'BADGE', 0, 4),
                                                                    ('Bonus Points', 'Extra points for consistency', 'POINTS', 100, 1);

-- Insert user connections (friendships)
INSERT INTO user_connections (user_id, connected_user_id, status) VALUES
                                                                      (2, 3, 'ACCEPTED'),  -- john_doe and jane_smith are friends
                                                                      (2, 4, 'ACCEPTED'),  -- john_doe and mike_wilson are friends
                                                                      (3, 5, 'ACCEPTED'),  -- jane_smith and sarah_connor are friends
                                                                      (4, 5, 'PENDING'),   -- mike_wilson sent request to sarah_connor
                                                                      (2, 5, 'ACCEPTED');  -- john_doe and sarah_connor are friends

-- Insert sample quiz questions
INSERT INTO quiz_questions (question, answer, difficulty, topic, source) VALUES
                                                                             ('What is the capital of France?', 'Paris', 'EASY', 'Geography', 'General Knowledge'),
                                                                             ('Who wrote "To Kill a Mockingbird"?', 'Harper Lee', 'MEDIUM', 'Literature', 'Classic Literature'),
                                                                             ('What is the chemical symbol for gold?', 'Au', 'MEDIUM', 'Chemistry', 'Science'),
                                                                             ('In which year did World War II end?', '1945', 'EASY', 'History', 'World History'),
                                                                             ('What is the largest planet in our solar system?', 'Jupiter', 'EASY', 'Astronomy', 'Science');

-- Insert sample quiz session
INSERT INTO quiz_sessions (user_id, challenge_id, difficulty, total_questions, correct_answers, status, started_at, completed_at) VALUES
    (3, 2, 'EASY', 5, 4, 'COMPLETED', CURRENT_TIMESTAMP - INTERVAL '1 hour', CURRENT_TIMESTAMP - INTERVAL '30 minutes');

-- Insert quiz rounds for the completed session
INSERT INTO quiz_rounds (quiz_session_id, question_id, user_answer, is_correct, round_number, answered_at, time_taken) VALUES
                                                                                                                           (1, 1, 'Paris', true, 1, CURRENT_TIMESTAMP - INTERVAL '55 minutes', 15),
                                                                                                                           (1, 2, 'Harper Lee', true, 2, CURRENT_TIMESTAMP - INTERVAL '50 minutes', 25),
                                                                                                                           (1, 3, 'Gold', false, 3, CURRENT_TIMESTAMP - INTERVAL '45 minutes', 20),
                                                                                                                           (1, 4, '1945', true, 4, CURRENT_TIMESTAMP - INTERVAL '40 minutes', 12),
                                                                                                                           (1, 5, 'Jupiter', true, 5, CURRENT_TIMESTAMP - INTERVAL '35 minutes', 18);

-- Insert some activity logs
INSERT INTO user_activity_logs (user_id, activity_type, description, metadata) VALUES
                                                                                   (2, 'CHALLENGE_JOINED', 'Joined 30-Day Fitness Challenge', '{"challenge_id": 1, "challenge_title": "30-Day Fitness Challenge"}'),
                                                                                   (3, 'QUEST_COMPLETED', 'Completed Spanish vocabulary practice', '{"quest_id": 2, "points_earned": 50}'),
                                                                                   (4, 'USER_CONNECTION', 'Connected with john_doe', '{"connected_user_id": 2, "connection_type": "friend"}'),
                                                                                   (5, 'CHALLENGE_PROGRESS', 'Updated progress on hiking challenge', '{"challenge_id": 3, "completion_percentage": 75.0}'),
                                                                                   (1, 'REWARD_EARNED', 'Earned Bookworm Badge', '{"reward_id": 4, "reward_type": "BADGE"}');

-- Grant sequence permissions (if needed)
-- This ensures the application can use sequences properly
ALTER SEQUENCE users_id_seq RESTART WITH 6;
ALTER SEQUENCE groups_id_seq RESTART WITH 5;
ALTER SEQUENCE challenges_id_seq RESTART WITH 5;
ALTER SEQUENCE challenge_progress_id_seq RESTART WITH 8;
ALTER SEQUENCE quests_id_seq RESTART WITH 5;
ALTER SEQUENCE tasks_id_seq RESTART WITH 5;
ALTER SEQUENCE rewards_id_seq RESTART WITH 6;
ALTER SEQUENCE user_connections_id_seq RESTART WITH 6;
ALTER SEQUENCE quiz_questions_id_seq RESTART WITH 6;
ALTER SEQUENCE quiz_sessions_id_seq RESTART WITH 2;
ALTER SEQUENCE quiz_rounds_id_seq RESTART WITH 6;
ALTER SEQUENCE user_activity_logs_id_seq RESTART WITH 6;