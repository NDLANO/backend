update imagemetadata set metadata = (
    SELECT jsonb_set(metadata, '{copyright,license}', '"PD"', true)

) where metadata -> 'copyright' ->> 'license' = 'publicdomain';