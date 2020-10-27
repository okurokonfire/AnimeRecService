-- public.ttagcategory definition

-- Drop table

-- DROP TABLE public.ttagcategory;

CREATE TABLE public.ttagcategory (
	categoryid serial NOT NULL,
	name varchar(255) NULL,
	CONSTRAINT ttagcategory_pkey PRIMARY KEY (categoryid)
);

-- public.tsource definition

-- Drop table

-- DROP TABLE public.tsource;

CREATE TABLE public.tsource (
	sourceid serial NOT NULL,
	name varchar(50) NULL,
	CONSTRAINT tsource_pkey PRIMARY KEY (sourceid)
);

-- public.tstatus definition

-- Drop table

-- DROP TABLE public.tstatus;

CREATE TABLE public.tstatus (
	statusid serial NOT NULL,
	name varchar(50) NULL,
	CONSTRAINT tstatus_pkey PRIMARY KEY (statusid)
);

-- public.tnametype definition

-- Drop table

-- DROP TABLE public.tnametype;

CREATE TABLE public.tnametype (
	nametypeid serial NOT NULL,
	name varchar(50) NULL,
	CONSTRAINT tnametype_pkey PRIMARY KEY (nametypeid)
);

-- public.tgenre definition

-- Drop table

-- DROP TABLE public.tgenre;

CREATE TABLE public.tgenre (
	genreid serial NOT NULL,
	name varchar(255) NULL,
	CONSTRAINT tgenre_pkey PRIMARY KEY (genreid)
);

-- public.tuser definition

-- Drop table

-- DROP TABLE public.tuser;

CREATE TABLE public.tuser (
	userid serial NOT NULL,
	anilistuserid int4 NULL,
	CONSTRAINT tuser_anilistuserid_key UNIQUE (anilistuserid),
	CONSTRAINT tuser_pkey PRIMARY KEY (userid)
);

-- public.tformat definition

-- Drop table

-- DROP TABLE public.tformat;

CREATE TABLE public.tformat (
	formatid serial NOT NULL,
	name varchar(50) NULL,
	CONSTRAINT tformat_pkey PRIMARY KEY (formatid)
);




-- public.tanime definition

-- Drop table

-- DROP TABLE public.tanime;

CREATE TABLE public.tanime (
	animeid serial NOT NULL,
	anilistanimeid int4 NULL,
	title varchar(500) NULL,
	datestart date NULL,
	dateend date NULL,
	episodes int4 NULL,
	formatid int4 NULL,
	sourceid int4 NULL,
	statusid int4 NULL,
	CONSTRAINT tanime_anilistanimeid_key UNIQUE (anilistanimeid),
	CONSTRAINT tanime_pkey PRIMARY KEY (animeid)
);


-- public.tanime foreign keys

ALTER TABLE public.tanime ADD CONSTRAINT fk_tanime_tformat FOREIGN KEY (formatid) REFERENCES tformat(formatid);
ALTER TABLE public.tanime ADD CONSTRAINT fk_tanime_tsource FOREIGN KEY (sourceid) REFERENCES tsource(sourceid);
ALTER TABLE public.tanime ADD CONSTRAINT fk_tanime_tstatus FOREIGN KEY (statusid) REFERENCES tstatus(statusid);

-- public.tanimealtname definition

-- Drop table

-- DROP TABLE public.tanimealtname;

CREATE TABLE public.tanimealtname (
	animealtnameid serial NOT NULL,
	animeid int4 NULL,
	animename varchar(500) NULL,
	nametypeid int4 NOT NULL,
	CONSTRAINT tanimealtname_pkey PRIMARY KEY (animealtnameid)
);


-- public.tanimealtname foreign keys

ALTER TABLE public.tanimealtname ADD CONSTRAINT fk_tanimealtname_tanime FOREIGN KEY (animeid) REFERENCES tanime(animeid);
ALTER TABLE public.tanimealtname ADD CONSTRAINT fk_tanimealtname_tnametype FOREIGN KEY (nametypeid) REFERENCES tnametype(nametypeid);

-- public.tanimelist definition

-- Drop table

-- DROP TABLE public.tanimelist;

CREATE TABLE public.tanimelist (
	userid int4 NOT NULL,
	animeid int4 NOT NULL,
	datestart date NULL,
	dateend date NULL,
	score int4 NULL,
	rewatched int4 NULL,
	CONSTRAINT tanimelist_pkey PRIMARY KEY (userid, animeid)
);


-- public.tanimelist foreign keys

ALTER TABLE public.tanimelist ADD CONSTRAINT fk_tanimelist_tanime FOREIGN KEY (animeid) REFERENCES tanime(animeid);
ALTER TABLE public.tanimelist ADD CONSTRAINT fk_tanimelist_tuser FOREIGN KEY (userid) REFERENCES tuser(userid);


-- public.tgenrelist definition

-- Drop table

-- DROP TABLE public.tgenrelist;

CREATE TABLE public.tgenrelist (
	animeid int4 NOT NULL,
	genreid int4 NOT NULL,
	CONSTRAINT tgenrelist_pkey PRIMARY KEY (animeid, genreid)
);


-- public.tgenrelist foreign keys

ALTER TABLE public.tgenrelist ADD CONSTRAINT tgenrelist_tanime FOREIGN KEY (animeid) REFERENCES tanime(animeid);
ALTER TABLE public.tgenrelist ADD CONSTRAINT tgenrelist_tgenre FOREIGN KEY (genreid) REFERENCES tgenre(genreid);


-- public.ttag definition

-- Drop table

-- DROP TABLE public.ttag;

CREATE TABLE public.ttag (
	tagid serial NOT NULL,
	anilisttagid int4 NULL,
	categoryid int4 NULL,
	name varchar(255) NULL,
	CONSTRAINT ttag_anilisttagid_key UNIQUE (anilisttagid),
	CONSTRAINT ttag_pkey PRIMARY KEY (tagid)
);


-- public.ttag foreign keys

ALTER TABLE public.ttag ADD CONSTRAINT fk_ttag_ttagcategory FOREIGN KEY (categoryid) REFERENCES ttagcategory(categoryid);



-- public.ttaglist definition

-- Drop table

-- DROP TABLE public.ttaglist;

CREATE TABLE public.ttaglist (
	animeid int4 NOT NULL,
	tagid int4 NOT NULL,
	CONSTRAINT ttaglist_pkey PRIMARY KEY (animeid, tagid)
);


-- public.ttaglist foreign keys

ALTER TABLE public.ttaglist ADD CONSTRAINT fk_ttaglist_tanime FOREIGN KEY (animeid) REFERENCES tanime(animeid);
ALTER TABLE public.ttaglist ADD CONSTRAINT fk_ttaglist_ttag FOREIGN KEY (tagid) REFERENCES ttag(tagid);

