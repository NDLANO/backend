UPDATE my_ndla_users
SET document=(
    document #- '{userRole}' || '{"userRole": "employee"}'
)
WHERE document ->> 'userRole' = 'teacher'
