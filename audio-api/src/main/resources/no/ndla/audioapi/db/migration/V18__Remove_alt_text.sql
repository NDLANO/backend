UPDATE seriesdata SET "document" = jsonb_set("document", '{coverPhoto,altText}', '""');

CREATE OR REPLACE FUNCTION remove_podcast_image_alt_text()
RETURNS VOID AS $$
DECLARE
    item_index INTEGER := 0;
    item_count INTEGER;
BEGIN
    -- Get the count of items in the list
    SELECT jsonb_array_length("document"->'podcastMeta')
    INTO item_count
    FROM audiodata;

    -- Loop over each item in the list
    WHILE item_index < item_count LOOP
        -- Update the nested field in the current item
        UPDATE audiodata
        SET "document" = jsonb_set("document", ARRAY['podcastMeta', item_index::text, 'coverPhoto', 'altText'], '""', true);

        -- Move to the next item
        item_index := item_index + 1;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

SELECT remove_podcast_image_alt_text();
DROP FUNCTION remove_podcast_image_alt_text();
