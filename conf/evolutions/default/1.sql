# --- !Ups

CREATE TABLE clubs (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  name         VARCHAR(255) NOT NULL,
  city         VARCHAR(255) NOT NULL,
  founded_year INT NOT NULL,
  stadium      VARCHAR(255) NOT NULL
);

CREATE TABLE players (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(255) NOT NULL,
  position    VARCHAR(100) NOT NULL,
  nationality VARCHAR(100) NOT NULL,
  age         INT NOT NULL,
  club_id     BIGINT NOT NULL,
  CONSTRAINT fk_players_club FOREIGN KEY (club_id) REFERENCES clubs(id)
);

# --- !Downs

DROP TABLE players;
DROP TABLE clubs;
