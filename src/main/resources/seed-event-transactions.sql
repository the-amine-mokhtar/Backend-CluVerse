-- Seed 10 event-based EXPENSE transactions
-- Run against the `cluverse` database:
--   mysql -u root cluverse < seed-event-transactions.sql

-- Pick the first club
SET @club_id = (SELECT id FROM club LIMIT 1);

-- Pick up to 3 events from that club (cycles if fewer than 3 exist)
SET @e1 = (SELECT id FROM `event` WHERE club_id = @club_id ORDER BY id LIMIT 1 OFFSET 0);
SET @e2 = (SELECT id FROM `event` WHERE club_id = @club_id ORDER BY id LIMIT 1 OFFSET 1);
SET @e3 = (SELECT id FROM `event` WHERE club_id = @club_id ORDER BY id LIMIT 1 OFFSET 2);

SET @e2 = IFNULL(@e2, @e1);
SET @e3 = IFNULL(@e3, @e1);

INSERT INTO `transaction` (amount, date, description, type, scope, club_id, event_id, budget_id, sponsor_id)
VALUES
  (350.00, '2025-01-15', 'Venue rental',                      'EXPENSE', 'EVENT', @club_id, @e1, NULL, NULL),
  (120.00, '2025-01-18', 'Catering & refreshments',           'EXPENSE', 'EVENT', @club_id, @e1, NULL, NULL),
  ( 85.00, '2025-01-22', 'Printed banners and signage',       'EXPENSE', 'EVENT', @club_id, @e1, NULL, NULL),
  (200.00, '2025-02-05', 'Speaker honorarium',                'EXPENSE', 'EVENT', @club_id, @e2, NULL, NULL),
  ( 60.00, '2025-02-08', 'AV equipment rental',              'EXPENSE', 'EVENT', @club_id, @e2, NULL, NULL),
  ( 45.00, '2025-02-11', 'Promotional flyers printing',       'EXPENSE', 'EVENT', @club_id, @e2, NULL, NULL),
  (180.00, '2025-03-12', 'Workshop materials & supplies',     'EXPENSE', 'EVENT', @club_id, @e3, NULL, NULL),
  ( 95.00, '2025-03-15', 'Transportation & logistics',        'EXPENSE', 'EVENT', @club_id, @e3, NULL, NULL),
  (220.00, '2025-03-21', 'Photography & videography',         'EXPENSE', 'EVENT', @club_id, @e3, NULL, NULL),
  ( 75.00, '2025-04-03', 'Social media ads for event promo',  'EXPENSE', 'EVENT', @club_id, @e1, NULL, NULL);

SELECT CONCAT('Inserted 10 transactions for club_id=', @club_id,
              ', events: ', IFNULL(@e1,'?'), ', ', IFNULL(@e2,'?'), ', ', IFNULL(@e3,'?')) AS result;
