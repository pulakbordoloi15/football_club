# --- !Ups

CREATE TABLE matches (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  home_club_id  BIGINT NOT NULL,
  away_club_id  BIGINT NOT NULL,
  scheduled_at  DATETIME NOT NULL,
  venue         VARCHAR(255) NOT NULL,
  status        VARCHAR(20) NOT NULL,
  home_score    INT,
  away_score    INT,
  CONSTRAINT fk_matches_home_club FOREIGN KEY (home_club_id) REFERENCES clubs(id),
  CONSTRAINT fk_matches_away_club FOREIGN KEY (away_club_id) REFERENCES clubs(id)
);

# --- !Downs

DROP TABLE matches;