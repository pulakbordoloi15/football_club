# --- !Ups

ALTER TABLE clubs ADD CONSTRAINT unique_club_name UNIQUE (name);
ALTER TABLE players ADD CONSTRAINT unique_player_name_club UNIQUE (name, club_id);

# --- !Downs

ALTER TABLE clubs DROP INDEX unique_club_name;
ALTER TABLE players DROP INDEX unique_player_name_club;