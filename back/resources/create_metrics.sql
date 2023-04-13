CREATE TABLE IF NOT EXISTS metrics (
  id INT GENERATED ALWAYS AS IDENTITY,
  exercise_id INT NOT NULL,
  name VARCHAR NOT NULL,
  value REAL NOT NULL,
  unit VARCHAR,
  note VARCHAR,
PRIMARY KEY(id),
CONSTRAINT fk_exercise_id
  FOREIGN KEY(exercise_id)
  REFERENCES exercises(id)
);
