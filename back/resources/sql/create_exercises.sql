CREATE TABLE IF NOT EXISTS exercises (
  id INT GENERATED ALWAYS AS IDENTITY,
  workout_id INT NOT NULL,
  type VARCHAR NOT NULL,
  note VARCHAR,
PRIMARY KEY(id),
CONSTRAINT fk_workout_id
  FOREIGN KEY(workout_id)
  REFERENCES workouts(id)
);
