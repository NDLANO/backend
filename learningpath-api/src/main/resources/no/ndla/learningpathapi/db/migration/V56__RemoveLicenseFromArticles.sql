update learningsteps
set document = document - 'copyright'
where document->>'articleId' is not null
  and document is not null
  and document ? 'copyright';
