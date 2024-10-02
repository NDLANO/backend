ALTER TABLE saved_shared_folder
    ALTER COLUMN "rank" DROP NOT NULL;

ALTER TABLE folders
    ALTER COLUMN "rank" DROP NOT NULL;

ALTER TABLE folder_resources
    ALTER COLUMN "rank" DROP NOT NULL;
