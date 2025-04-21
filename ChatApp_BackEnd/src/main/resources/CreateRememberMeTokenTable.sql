DROP TABLE IF EXISTS public.persistent_logins;

CREATE TABLE IF NOT EXISTS public.persistent_logins (
	username varchar(255) NOT NULL,
	series varchar(255) PRIMARY KEY,
	token varchar(255) NOT NULL,
	last_used timestamp without time zone NOT NULL
); 