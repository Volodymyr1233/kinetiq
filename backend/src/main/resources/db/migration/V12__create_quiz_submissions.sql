create table quiz_submissions (
    id            uuid         not null,
    client_id     uuid         not null,
    goal          varchar(32)  not null,
    experience    varchar(32)  not null,
    level         varchar(32)  not null,
    age           integer      not null,
    height_cm     integer      not null,
    weight_kg     numeric(4,1) not null,
    days_per_week integer      not null,
    notes         varchar(500),
    submitted_at  timestamptz  not null,
    created_at    timestamptz  not null,
    updated_at    timestamptz  not null,
    version       bigint       not null,
    constraint pk_quiz_submissions primary key (id),
    constraint fk_quiz_submissions_client foreign key (client_id) references accounts (id),
    constraint uq_quiz_submissions_client unique (client_id),
    constraint ck_quiz_submissions_goal check (goal in (
        'BUILD_MUSCLE', 'GET_STRONGER', 'LOSE_FAT', 'MOVE_BETTER', 'GENERAL_FITNESS')),
    constraint ck_quiz_submissions_experience check (experience in (
        'LESS_THAN_3_MONTHS', 'MONTHS_3_TO_6', 'MONTHS_6_TO_12', 'YEARS_1_TO_2', 'YEARS_2_TO_5', 'YEARS_5_PLUS')),
    constraint ck_quiz_submissions_level check (level in ('BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'ATHLETE')),
    constraint ck_quiz_submissions_age check (age between 16 and 99),
    constraint ck_quiz_submissions_height_cm check (height_cm between 120 and 230),
    constraint ck_quiz_submissions_weight_kg check (weight_kg between 30.0 and 250.0),
    constraint ck_quiz_submissions_days_per_week check (days_per_week between 2 and 6)
);

create table quiz_submission_likes (
    submission_id uuid        not null,
    like_code     varchar(32) not null,
    constraint pk_quiz_submission_likes primary key (submission_id, like_code),
    constraint fk_quiz_submission_likes_submission foreign key (submission_id) references quiz_submissions (id),
    constraint ck_quiz_submission_likes_like_code check (like_code in (
        'WEIGHTS', 'CARDIO', 'HIIT', 'YOGA_PILATES', 'BODYWEIGHT'))
);
