delete
from imagefiledata i2
where id != (
	select max(id)
	from imagefiledata i
	where i.image_meta_id = i2.image_meta_id
	and i.metadata->>'language' = i2.metadata->>'language'
);
