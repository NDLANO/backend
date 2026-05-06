update subjectpage
set document = document || '{"popularArticles": []}'::jsonb
where not (document ? 'popularArticles');
