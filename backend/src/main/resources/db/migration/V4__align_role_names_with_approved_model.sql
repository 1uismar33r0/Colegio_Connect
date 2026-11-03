-- Align persisted role identifiers with the approved two-account-type, four-role model.
-- Existing assignments retain their meaning while gaining the canonical identifiers.

ALTER TABLE role_assignments DROP CONSTRAINT ck_role_assignments_role;

UPDATE role_assignments
SET role = CASE role
    WHEN 'INSTITUTIONAL' THEN 'FACULTY_OFFICE_DEPT'
    WHEN 'COMPANY' THEN 'EXTERNAL_COMPANY'
    WHEN 'MODERATOR' THEN 'ADMIN_MODERATOR'
    ELSE role
END;

ALTER TABLE role_assignments
    ADD CONSTRAINT ck_role_assignments_role
    CHECK (role IN ('STUDENT', 'FACULTY_OFFICE_DEPT', 'EXTERNAL_COMPANY', 'ADMIN_MODERATOR'));
