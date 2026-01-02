-- ============================================================================
-- Seed Data: Hierarchical Topic Tree for Quiz Questions
-- File: src/main/resources/db/migration/V16__Seed_Topic_Hierarchy.sql
--
-- This creates a comprehensive topic tree with 4 levels of depth covering
-- major knowledge domains for quiz questions.
-- ============================================================================

-- ============================================================================
-- HELPER: Disable triggers temporarily for bulk insert performance
-- ============================================================================
ALTER TABLE topics DISABLE TRIGGER trg_topic_hierarchy_insert;

-- ============================================================================
-- LEVEL 0: ROOT TOPICS (12 main categories)
-- ============================================================================

INSERT INTO topics (id, name, slug, description, parent_id, path, depth, is_system_topic, validation_status, is_active, question_count, created_at) VALUES

-- 1. GEOGRAPHY
(1, 'Geography', 'geography', 'Study of Earth''s landscapes, environments, and the relationships between people and their environments', NULL, '/1/', 0, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),

-- 2. HISTORY
(2, 'History', 'history', 'Study of past events, particularly human affairs', NULL, '/2/', 0, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),

-- 3. SCIENCE
(3, 'Science', 'science', 'Systematic study of the natural world through observation and experiment', NULL, '/3/', 0, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),

-- 4. ARTS & CULTURE
(4, 'Arts & Culture', 'arts-culture', 'Visual arts, performing arts, literature, and cultural traditions', NULL, '/4/', 0, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),

-- 5. SPORTS & GAMES
(5, 'Sports & Games', 'sports-games', 'Athletic competitions, board games, and recreational activities', NULL, '/5/', 0, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),

-- 6. ENTERTAINMENT
(6, 'Entertainment', 'entertainment', 'Movies, TV shows, music, and popular media', NULL, '/6/', 0, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),

-- 7. TECHNOLOGY
(7, 'Technology', 'technology', 'Computers, internet, gadgets, and technological innovations', NULL, '/7/', 0, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),

-- 8. NATURE & WILDLIFE
(8, 'Nature & Wildlife', 'nature-wildlife', 'Animals, plants, ecosystems, and the natural environment', NULL, '/8/', 0, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),

-- 9. FOOD & DRINK
(9, 'Food & Drink', 'food-drink', 'Cuisine, beverages, cooking, and culinary traditions', NULL, '/9/', 0, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),

-- 10. LANGUAGE & LITERATURE
(10, 'Language & Literature', 'language-literature', 'Languages, linguistics, books, and written works', NULL, '/10/', 0, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),

-- 11. MYTHOLOGY & RELIGION
(11, 'Mythology & Religion', 'mythology-religion', 'World religions, myths, legends, and spiritual traditions', NULL, '/11/', 0, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),

-- 12. GENERAL KNOWLEDGE
(12, 'General Knowledge', 'general-knowledge', 'Miscellaneous facts and trivia across various domains', NULL, '/12/', 0, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP);

-- ============================================================================
-- LEVEL 1: GEOGRAPHY SUBTOPICS
-- ============================================================================

INSERT INTO topics (id, name, slug, description, parent_id, path, depth, is_system_topic, validation_status, is_active, question_count, created_at) VALUES

-- Physical Geography (under Geography)
(101, 'Physical Geography', 'geography-physical', 'Natural features and processes of the Earth', 1, '/1/101/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
(102, 'Human Geography', 'geography-human', 'Human populations, cultures, and their interactions with the environment', 1, '/1/102/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
(103, 'Countries & Capitals', 'geography-countries-capitals', 'Nations of the world and their capital cities', 1, '/1/103/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
(104, 'Oceans & Seas', 'geography-oceans-seas', 'Bodies of water around the world', 1, '/1/104/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
(105, 'Mountains & Ranges', 'geography-mountains-ranges', 'Mountain peaks and ranges worldwide', 1, '/1/105/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
(106, 'Rivers & Lakes', 'geography-rivers-lakes', 'Major waterways and freshwater bodies', 1, '/1/106/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
(107, 'Flags & Symbols', 'geography-flags-symbols', 'National flags and country symbols', 1, '/1/107/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
(108, 'Maps & Cartography', 'geography-maps-cartography', 'Map reading and mapmaking', 1, '/1/108/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP);

-- ============================================================================
-- LEVEL 2: PHYSICAL GEOGRAPHY SUBTOPICS
-- ============================================================================

INSERT INTO topics (id, name, slug, description, parent_id, path, depth, is_system_topic, validation_status, is_active, question_count, created_at) VALUES

                                                                                                                                                        (1011, 'Geology', 'geography-physical-geology', 'Study of Earth''s solid matter, rocks, and geological processes', 101, '/1/101/1011/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (1012, 'Climatology', 'geography-physical-climatology', 'Study of climate patterns and atmospheric conditions', 101, '/1/101/1012/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (1013, 'Geomorphology', 'geography-physical-geomorphology', 'Study of landforms and the processes that shape them', 101, '/1/101/1013/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (1014, 'Biogeography', 'geography-physical-biogeography', 'Distribution of species and ecosystems', 101, '/1/101/1014/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP);

-- ============================================================================
-- LEVEL 3: GEOLOGY SUBTOPICS
-- ============================================================================

INSERT INTO topics (id, name, slug, description, parent_id, path, depth, is_system_topic, validation_status, is_active, question_count, created_at) VALUES

                                                                                                                                                        (10111, 'Minerals', 'geography-physical-geology-minerals', 'Natural inorganic substances with specific chemical compositions', 1011, '/1/101/1011/10111/', 3, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (10112, 'Rocks & Rock Types', 'geography-physical-geology-rocks', 'Igneous, sedimentary, and metamorphic rocks', 1011, '/1/101/1011/10112/', 3, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (10113, 'Plate Tectonics', 'geography-physical-geology-tectonics', 'Movement of Earth''s lithospheric plates', 1011, '/1/101/1011/10113/', 3, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (10114, 'Volcanoes', 'geography-physical-geology-volcanoes', 'Volcanic formations and eruptions', 1011, '/1/101/1011/10114/', 3, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (10115, 'Earthquakes', 'geography-physical-geology-earthquakes', 'Seismic activity and earthquake phenomena', 1011, '/1/101/1011/10115/', 3, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (10116, 'Fossils & Paleontology', 'geography-physical-geology-fossils', 'Preserved remains of ancient organisms', 1011, '/1/101/1011/10116/', 3, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP);

-- ============================================================================
-- LEVEL 1: HISTORY SUBTOPICS
-- ============================================================================

INSERT INTO topics (id, name, slug, description, parent_id, path, depth, is_system_topic, validation_status, is_active, question_count, created_at) VALUES

                                                                                                                                                        (201, 'Ancient History', 'history-ancient', 'Events before the fall of Rome (476 AD)', 2, '/2/201/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (202, 'Medieval History', 'history-medieval', 'Middle Ages (500-1500 AD)', 2, '/2/202/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (203, 'Modern History', 'history-modern', 'Events from 1500 AD to present', 2, '/2/203/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (204, 'World Wars', 'history-world-wars', 'World War I and World War II', 2, '/2/204/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (205, 'American History', 'history-american', 'History of the United States', 2, '/2/205/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (206, 'European History', 'history-european', 'History of European nations', 2, '/2/206/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (207, 'Asian History', 'history-asian', 'History of Asian civilizations', 2, '/2/207/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (208, 'Military History', 'history-military', 'Wars, battles, and military leaders', 2, '/2/208/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (209, 'Historical Figures', 'history-figures', 'Famous people throughout history', 2, '/2/209/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP);

-- ============================================================================
-- LEVEL 2: ANCIENT HISTORY SUBTOPICS
-- ============================================================================

INSERT INTO topics (id, name, slug, description, parent_id, path, depth, is_system_topic, validation_status, is_active, question_count, created_at) VALUES

                                                                                                                                                        (2011, 'Ancient Egypt', 'history-ancient-egypt', 'Pharaohs, pyramids, and Nile civilization', 201, '/2/201/2011/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (2012, 'Ancient Greece', 'history-ancient-greece', 'Greek city-states, philosophy, and democracy', 201, '/2/201/2012/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (2013, 'Ancient Rome', 'history-ancient-rome', 'Roman Republic and Empire', 201, '/2/201/2013/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (2014, 'Ancient Mesopotamia', 'history-ancient-mesopotamia', 'Sumerians, Babylonians, and Assyrians', 201, '/2/201/2014/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (2015, 'Ancient China', 'history-ancient-china', 'Chinese dynasties and civilization', 201, '/2/201/2015/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (2016, 'Ancient India', 'history-ancient-india', 'Indus Valley and early Indian civilizations', 201, '/2/201/2016/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP);

-- ============================================================================
-- LEVEL 2: WORLD WARS SUBTOPICS
-- ============================================================================

INSERT INTO topics (id, name, slug, description, parent_id, path, depth, is_system_topic, validation_status, is_active, question_count, created_at) VALUES

                                                                                                                                                        (2041, 'World War I', 'history-world-wars-ww1', 'The Great War (1914-1918)', 204, '/2/204/2041/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (2042, 'World War II', 'history-world-wars-ww2', 'The Second World War (1939-1945)', 204, '/2/204/2042/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (2043, 'Holocaust', 'history-world-wars-holocaust', 'Genocide during WWII', 204, '/2/204/2043/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP);

-- ============================================================================
-- LEVEL 3: WORLD WAR II SUBTOPICS
-- ============================================================================

INSERT INTO topics (id, name, slug, description, parent_id, path, depth, is_system_topic, validation_status, is_active, question_count, created_at) VALUES

                                                                                                                                                        (20421, 'D-Day & European Theater', 'history-ww2-european', 'Allied operations in Europe', 2042, '/2/204/2042/20421/', 3, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (20422, 'Pacific Theater', 'history-ww2-pacific', 'War in the Pacific', 2042, '/2/204/2042/20422/', 3, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (20423, 'WWII Leaders', 'history-ww2-leaders', 'Political and military leaders of WWII', 2042, '/2/204/2042/20423/', 3, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (20424, 'WWII Technology', 'history-ww2-technology', 'Weapons and technology of WWII', 2042, '/2/204/2042/20424/', 3, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP);

-- ============================================================================
-- LEVEL 1: SCIENCE SUBTOPICS
-- ============================================================================

INSERT INTO topics (id, name, slug, description, parent_id, path, depth, is_system_topic, validation_status, is_active, question_count, created_at) VALUES

                                                                                                                                                        (301, 'Biology', 'science-biology', 'Study of living organisms', 3, '/3/301/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (302, 'Chemistry', 'science-chemistry', 'Study of matter and its properties', 3, '/3/302/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (303, 'Physics', 'science-physics', 'Study of matter, energy, and fundamental forces', 3, '/3/303/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (304, 'Astronomy', 'science-astronomy', 'Study of celestial objects and space', 3, '/3/304/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (305, 'Medicine & Health', 'science-medicine', 'Human health and medical science', 3, '/3/305/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (306, 'Mathematics', 'science-mathematics', 'Numbers, equations, and mathematical concepts', 3, '/3/306/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (307, 'Environmental Science', 'science-environmental', 'Ecology and environmental issues', 3, '/3/307/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (308, 'Earth Science', 'science-earth', 'Study of Earth and its systems', 3, '/3/308/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP);

-- ============================================================================
-- LEVEL 2: BIOLOGY SUBTOPICS
-- ============================================================================

INSERT INTO topics (id, name, slug, description, parent_id, path, depth, is_system_topic, validation_status, is_active, question_count, created_at) VALUES

                                                                                                                                                        (3011, 'Human Anatomy', 'science-biology-anatomy', 'Structure of the human body', 301, '/3/301/3011/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (3012, 'Genetics', 'science-biology-genetics', 'DNA, heredity, and genetic variation', 301, '/3/301/3012/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (3013, 'Microbiology', 'science-biology-microbiology', 'Study of microorganisms', 301, '/3/301/3013/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (3014, 'Zoology', 'science-biology-zoology', 'Study of animals', 301, '/3/301/3014/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (3015, 'Botany', 'science-biology-botany', 'Study of plants', 301, '/3/301/3015/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (3016, 'Evolution', 'science-biology-evolution', 'Theory of evolution and natural selection', 301, '/3/301/3016/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (3017, 'Cell Biology', 'science-biology-cell', 'Structure and function of cells', 301, '/3/301/3017/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP);

-- ============================================================================
-- LEVEL 2: ASTRONOMY SUBTOPICS
-- ============================================================================

INSERT INTO topics (id, name, slug, description, parent_id, path, depth, is_system_topic, validation_status, is_active, question_count, created_at) VALUES

                                                                                                                                                        (3041, 'Solar System', 'science-astronomy-solar-system', 'Sun, planets, moons, and asteroids', 304, '/3/304/3041/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (3042, 'Stars & Galaxies', 'science-astronomy-stars-galaxies', 'Stellar objects and galactic structures', 304, '/3/304/3042/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (3043, 'Space Exploration', 'science-astronomy-space-exploration', 'Human spaceflight and missions', 304, '/3/304/3043/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (3044, 'Cosmology', 'science-astronomy-cosmology', 'Origin and structure of the universe', 304, '/3/304/3044/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP);

-- ============================================================================
-- LEVEL 3: SOLAR SYSTEM SUBTOPICS
-- ============================================================================

INSERT INTO topics (id, name, slug, description, parent_id, path, depth, is_system_topic, validation_status, is_active, question_count, created_at) VALUES

                                                                                                                                                        (30411, 'The Sun', 'science-astronomy-solar-system-sun', 'Our star and its properties', 3041, '/3/304/3041/30411/', 3, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (30412, 'Inner Planets', 'science-astronomy-solar-system-inner', 'Mercury, Venus, Earth, and Mars', 3041, '/3/304/3041/30412/', 3, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (30413, 'Outer Planets', 'science-astronomy-solar-system-outer', 'Jupiter, Saturn, Uranus, and Neptune', 3041, '/3/304/3041/30413/', 3, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (30414, 'Moons', 'science-astronomy-solar-system-moons', 'Natural satellites of planets', 3041, '/3/304/3041/30414/', 3, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (30415, 'Dwarf Planets', 'science-astronomy-solar-system-dwarf', 'Pluto, Ceres, Eris, and others', 3041, '/3/304/3041/30415/', 3, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP);

-- ============================================================================
-- LEVEL 1: ARTS & CULTURE SUBTOPICS
-- ============================================================================

INSERT INTO topics (id, name, slug, description, parent_id, path, depth, is_system_topic, validation_status, is_active, question_count, created_at) VALUES

                                                                                                                                                        (401, 'Visual Arts', 'arts-visual', 'Painting, sculpture, and visual media', 4, '/4/401/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (402, 'Music', 'arts-music', 'Musical genres, artists, and history', 4, '/4/402/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (403, 'Architecture', 'arts-architecture', 'Building design and famous structures', 4, '/4/403/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (404, 'Theater & Dance', 'arts-theater-dance', 'Performing arts and stage productions', 4, '/4/404/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (405, 'Photography', 'arts-photography', 'Art and history of photography', 4, '/4/405/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (406, 'Fashion', 'arts-fashion', 'Fashion history and designers', 4, '/4/406/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP);

-- ============================================================================
-- LEVEL 2: MUSIC SUBTOPICS
-- ============================================================================

INSERT INTO topics (id, name, slug, description, parent_id, path, depth, is_system_topic, validation_status, is_active, question_count, created_at) VALUES

                                                                                                                                                        (4021, 'Classical Music', 'arts-music-classical', 'Classical composers and compositions', 402, '/4/402/4021/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (4022, 'Rock & Pop', 'arts-music-rock-pop', 'Rock and pop music history', 402, '/4/402/4022/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (4023, 'Jazz & Blues', 'arts-music-jazz-blues', 'Jazz and blues music', 402, '/4/402/4023/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (4024, 'Hip Hop & R&B', 'arts-music-hiphop-rnb', 'Hip hop and R&B music', 402, '/4/402/4024/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (4025, 'World Music', 'arts-music-world', 'Traditional and folk music worldwide', 402, '/4/402/4025/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (4026, 'Musical Instruments', 'arts-music-instruments', 'Types and history of instruments', 402, '/4/402/4026/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP);

-- ============================================================================
-- LEVEL 1: SPORTS & GAMES SUBTOPICS
-- ============================================================================

INSERT INTO topics (id, name, slug, description, parent_id, path, depth, is_system_topic, validation_status, is_active, question_count, created_at) VALUES

                                                                                                                                                        (501, 'Football (Soccer)', 'sports-football', 'Association football worldwide', 5, '/5/501/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (502, 'American Football', 'sports-american-football', 'NFL and American football', 5, '/5/502/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (503, 'Basketball', 'sports-basketball', 'NBA and basketball worldwide', 5, '/5/503/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (504, 'Baseball', 'sports-baseball', 'MLB and baseball history', 5, '/5/504/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (505, 'Tennis', 'sports-tennis', 'Grand Slams and tennis history', 5, '/5/505/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (506, 'Olympics', 'sports-olympics', 'Olympic Games history and records', 5, '/5/506/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (507, 'Combat Sports', 'sports-combat', 'Boxing, MMA, wrestling', 5, '/5/507/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (508, 'Motor Sports', 'sports-motor', 'Racing and motorsports', 5, '/5/508/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (509, 'Winter Sports', 'sports-winter', 'Skiing, hockey, skating', 5, '/5/509/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (510, 'Board Games & Chess', 'sports-board-games', 'Strategy games and chess', 5, '/5/510/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (511, 'E-Sports', 'sports-esports', 'Competitive video gaming', 5, '/5/511/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP);

-- ============================================================================
-- LEVEL 2: OLYMPICS SUBTOPICS
-- ============================================================================

INSERT INTO topics (id, name, slug, description, parent_id, path, depth, is_system_topic, validation_status, is_active, question_count, created_at) VALUES

                                                                                                                                                        (5061, 'Summer Olympics', 'sports-olympics-summer', 'Summer Olympic Games', 506, '/5/506/5061/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (5062, 'Winter Olympics', 'sports-olympics-winter', 'Winter Olympic Games', 506, '/5/506/5062/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (5063, 'Olympic Records', 'sports-olympics-records', 'Olympic records and achievements', 506, '/5/506/5063/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (5064, 'Olympic Athletes', 'sports-olympics-athletes', 'Famous Olympic athletes', 506, '/5/506/5064/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP);

-- ============================================================================
-- LEVEL 1: ENTERTAINMENT SUBTOPICS
-- ============================================================================

INSERT INTO topics (id, name, slug, description, parent_id, path, depth, is_system_topic, validation_status, is_active, question_count, created_at) VALUES

                                                                                                                                                        (601, 'Movies', 'entertainment-movies', 'Films and cinema', 6, '/6/601/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (602, 'Television', 'entertainment-television', 'TV shows and series', 6, '/6/602/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (603, 'Celebrities', 'entertainment-celebrities', 'Famous personalities', 6, '/6/603/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (604, 'Video Games', 'entertainment-video-games', 'Gaming history and culture', 6, '/6/604/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (605, 'Comics & Animation', 'entertainment-comics-animation', 'Comics, manga, and animated content', 6, '/6/605/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (606, 'Awards & Ceremonies', 'entertainment-awards', 'Oscars, Emmys, and other awards', 6, '/6/606/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP);

-- ============================================================================
-- LEVEL 2: MOVIES SUBTOPICS
-- ============================================================================

INSERT INTO topics (id, name, slug, description, parent_id, path, depth, is_system_topic, validation_status, is_active, question_count, created_at) VALUES

                                                                                                                                                        (6011, 'Classic Films', 'entertainment-movies-classic', 'Classic and vintage cinema', 601, '/6/601/6011/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (6012, 'Action & Adventure', 'entertainment-movies-action', 'Action and adventure films', 601, '/6/601/6012/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (6013, 'Science Fiction', 'entertainment-movies-scifi', 'Sci-fi movies', 601, '/6/601/6013/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (6014, 'Horror', 'entertainment-movies-horror', 'Horror films', 601, '/6/601/6014/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (6015, 'Comedy', 'entertainment-movies-comedy', 'Comedy films', 601, '/6/601/6015/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (6016, 'Disney & Pixar', 'entertainment-movies-disney', 'Disney and Pixar animated films', 601, '/6/601/6016/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (6017, 'Marvel & DC', 'entertainment-movies-superhero', 'Superhero movies', 601, '/6/601/6017/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (6018, 'Directors & Filmmakers', 'entertainment-movies-directors', 'Famous film directors', 601, '/6/601/6018/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP);

-- ============================================================================
-- LEVEL 1: TECHNOLOGY SUBTOPICS
-- ============================================================================

INSERT INTO topics (id, name, slug, description, parent_id, path, depth, is_system_topic, validation_status, is_active, question_count, created_at) VALUES

                                                                                                                                                        (701, 'Computers', 'technology-computers', 'Computer hardware and software', 7, '/7/701/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (702, 'Internet', 'technology-internet', 'World Wide Web and online services', 7, '/7/702/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (703, 'Mobile Technology', 'technology-mobile', 'Smartphones and mobile devices', 7, '/7/703/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (704, 'Artificial Intelligence', 'technology-ai', 'AI and machine learning', 7, '/7/704/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (705, 'Inventions', 'technology-inventions', 'Famous inventions and inventors', 7, '/7/705/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (706, 'Social Media', 'technology-social-media', 'Social networking platforms', 7, '/7/706/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (707, 'Cybersecurity', 'technology-cybersecurity', 'Computer and network security', 7, '/7/707/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP);

-- ============================================================================
-- LEVEL 1: NATURE & WILDLIFE SUBTOPICS
-- ============================================================================

INSERT INTO topics (id, name, slug, description, parent_id, path, depth, is_system_topic, validation_status, is_active, question_count, created_at) VALUES

                                                                                                                                                        (801, 'Mammals', 'nature-mammals', 'Warm-blooded animals', 8, '/8/801/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (802, 'Birds', 'nature-birds', 'Avian species', 8, '/8/802/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (803, 'Reptiles & Amphibians', 'nature-reptiles-amphibians', 'Cold-blooded vertebrates', 8, '/8/803/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (804, 'Marine Life', 'nature-marine', 'Ocean animals and ecosystems', 8, '/8/804/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (805, 'Insects & Arachnids', 'nature-insects', 'Bugs and spiders', 8, '/8/805/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (806, 'Plants & Trees', 'nature-plants', 'Flora and vegetation', 8, '/8/806/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (807, 'Endangered Species', 'nature-endangered', 'Threatened and endangered wildlife', 8, '/8/807/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (808, 'Dinosaurs', 'nature-dinosaurs', 'Prehistoric animals', 8, '/8/808/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP);

-- ============================================================================
-- LEVEL 2: MAMMALS SUBTOPICS
-- ============================================================================

INSERT INTO topics (id, name, slug, description, parent_id, path, depth, is_system_topic, validation_status, is_active, question_count, created_at) VALUES

                                                                                                                                                        (8011, 'Big Cats', 'nature-mammals-big-cats', 'Lions, tigers, leopards, and jaguars', 801, '/8/801/8011/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (8012, 'Primates', 'nature-mammals-primates', 'Apes, monkeys, and lemurs', 801, '/8/801/8012/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (8013, 'Marine Mammals', 'nature-mammals-marine', 'Whales, dolphins, and seals', 801, '/8/801/8013/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (8014, 'Domestic Animals', 'nature-mammals-domestic', 'Pets and farm animals', 801, '/8/801/8014/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP);

-- ============================================================================
-- LEVEL 1: FOOD & DRINK SUBTOPICS
-- ============================================================================

INSERT INTO topics (id, name, slug, description, parent_id, path, depth, is_system_topic, validation_status, is_active, question_count, created_at) VALUES

                                                                                                                                                        (901, 'World Cuisines', 'food-world-cuisines', 'Traditional foods from around the world', 9, '/9/901/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (902, 'Beverages', 'food-beverages', 'Drinks and brewing', 9, '/9/902/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (903, 'Cooking & Techniques', 'food-cooking', 'Culinary methods and skills', 9, '/9/903/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (904, 'Ingredients', 'food-ingredients', 'Food items and produce', 9, '/9/904/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (905, 'Famous Chefs', 'food-chefs', 'Renowned culinary personalities', 9, '/9/905/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (906, 'Food History', 'food-history', 'Origins and history of foods', 9, '/9/906/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP);

-- ============================================================================
-- LEVEL 2: BEVERAGES SUBTOPICS
-- ============================================================================

INSERT INTO topics (id, name, slug, description, parent_id, path, depth, is_system_topic, validation_status, is_active, question_count, created_at) VALUES

                                                                                                                                                        (9021, 'Wine', 'food-beverages-wine', 'Wine varieties and regions', 902, '/9/902/9021/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (9022, 'Beer', 'food-beverages-beer', 'Beer types and brewing', 902, '/9/902/9022/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (9023, 'Spirits', 'food-beverages-spirits', 'Whiskey, vodka, rum, and more', 902, '/9/902/9023/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (9024, 'Coffee & Tea', 'food-beverages-coffee-tea', 'Hot beverages and their origins', 902, '/9/902/9024/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (9025, 'Cocktails', 'food-beverages-cocktails', 'Mixed drinks and bartending', 902, '/9/902/9025/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP);

-- ============================================================================
-- LEVEL 1: LANGUAGE & LITERATURE SUBTOPICS
-- ============================================================================

INSERT INTO topics (id, name, slug, description, parent_id, path, depth, is_system_topic, validation_status, is_active, question_count, created_at) VALUES

                                                                                                                                                        (1001, 'Classic Literature', 'literature-classic', 'Classic novels and authors', 10, '/10/1001/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (1002, 'Modern Literature', 'literature-modern', 'Contemporary books and authors', 10, '/10/1002/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (1003, 'Poetry', 'literature-poetry', 'Poets and poetry', 10, '/10/1003/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (1004, 'Languages', 'literature-languages', 'World languages and linguistics', 10, '/10/1004/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (1005, 'Famous Authors', 'literature-authors', 'Renowned writers', 10, '/10/1005/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (1006, 'Children''s Literature', 'literature-children', 'Books for young readers', 10, '/10/1006/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (1007, 'Fantasy & Sci-Fi Literature', 'literature-fantasy-scifi', 'Fantasy and science fiction books', 10, '/10/1007/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP);

-- ============================================================================
-- LEVEL 1: MYTHOLOGY & RELIGION SUBTOPICS
-- ============================================================================

INSERT INTO topics (id, name, slug, description, parent_id, path, depth, is_system_topic, validation_status, is_active, question_count, created_at) VALUES

                                                                                                                                                        (1101, 'Greek Mythology', 'mythology-greek', 'Gods and heroes of ancient Greece', 11, '/11/1101/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (1102, 'Roman Mythology', 'mythology-roman', 'Roman gods and legends', 11, '/11/1102/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (1103, 'Norse Mythology', 'mythology-norse', 'Vikings and Norse gods', 11, '/11/1103/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (1104, 'Egyptian Mythology', 'mythology-egyptian', 'Ancient Egyptian gods', 11, '/11/1104/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (1105, 'World Religions', 'mythology-world-religions', 'Major religions of the world', 11, '/11/1105/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (1106, 'Folklore & Legends', 'mythology-folklore', 'Traditional stories and legends', 11, '/11/1106/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP);

-- ============================================================================
-- LEVEL 2: GREEK MYTHOLOGY SUBTOPICS
-- ============================================================================

INSERT INTO topics (id, name, slug, description, parent_id, path, depth, is_system_topic, validation_status, is_active, question_count, created_at) VALUES

                                                                                                                                                        (11011, 'Olympian Gods', 'mythology-greek-olympians', 'The twelve Olympian deities', 1101, '/11/1101/11011/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (11012, 'Greek Heroes', 'mythology-greek-heroes', 'Hercules, Perseus, Achilles, and more', 1101, '/11/1101/11012/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (11013, 'Mythical Creatures', 'mythology-greek-creatures', 'Centaurs, Minotaurs, and more', 1101, '/11/1101/11013/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (11014, 'Trojan War', 'mythology-greek-trojan-war', 'The epic war of Troy', 1101, '/11/1101/11014/', 2, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP);

-- ============================================================================
-- LEVEL 1: GENERAL KNOWLEDGE SUBTOPICS
-- ============================================================================

INSERT INTO topics (id, name, slug, description, parent_id, path, depth, is_system_topic, validation_status, is_active, question_count, created_at) VALUES

                                                                                                                                                        (1201, 'World Records', 'general-world-records', 'Guinness World Records and achievements', 12, '/12/1201/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (1202, 'Famous Quotes', 'general-quotes', 'Notable quotations and sayings', 12, '/12/1202/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (1203, 'Current Events', 'general-current-events', 'Recent news and happenings', 12, '/12/1203/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (1204, 'Pop Culture', 'general-pop-culture', 'Trending topics and cultural phenomena', 12, '/12/1204/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (1205, 'Business & Economics', 'general-business', 'Commerce and economic concepts', 12, '/12/1205/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (1206, 'Law & Government', 'general-law-government', 'Legal systems and governance', 12, '/12/1206/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (1207, 'Psychology', 'general-psychology', 'Human behavior and mind', 12, '/12/1207/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP),
                                                                                                                                                        (1208, 'Philosophy', 'general-philosophy', 'Philosophical concepts and thinkers', 12, '/12/1208/', 1, true, 'APPROVED', true, 0, CURRENT_TIMESTAMP);

-- ============================================================================
-- Re-enable triggers
-- ============================================================================
ALTER TABLE topics ENABLE TRIGGER trg_topic_hierarchy_insert;

-- ============================================================================
-- Update sequence to avoid conflicts with future inserts
-- ============================================================================
SELECT setval('topics_id_seq', (SELECT MAX(id) FROM topics) + 1);

-- ============================================================================
-- Update question counts (if quiz_questions table exists with topic_id)
-- ============================================================================
UPDATE topics t
SET question_count = (
    SELECT COUNT(*)
    FROM quiz_questions q
    WHERE q.topic_id = t.id AND q.is_active = true
);