update articledata
set document = document - 'prioritized'
where document is not null
  and document ? 'prioritized';
