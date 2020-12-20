DROP TABLE public.tstafflist;

DROP TABLE public.tstudiolist;

DROP TABLE public.ttaglist;

DROP TABLE public.ttag;

DROP TABLE public.tgenrelist;

DROP TABLE public.tanimelist;

DROP TABLE public.tanimealtname;

DROP TABLE public.tanimerecs;

DROP TABLE public.tanime;

DROP TABLE public.tformat;

DROP TABLE public.tuser;

DROP TABLE public.tgenre;

DROP TABLE public.tnametype;

DROP TABLE public.twatchstatus;

DROP TABLE public.tstatus;

DROP TABLE public.tsource;

DROP TABLE public.ttagcategory;

DROP TABLE public.tstudio;

DROP TABLE public.tstaff;

DROP TABLE public.tmediatype;

DROP TABLE public.counters;

CREATE TABLE public.counters (
	counterid int4 NOT NULL,
	countername varchar(255) NULL,
	value int4 NULL,
	CONSTRAINT tcounters_pkey PRIMARY KEY (counterid)
);

-- public.tstaff definition

CREATE TABLE public.tstaff (
	staffid serial NOT NULL,
	aniliststaffid int4 NOT NULL,
	firstname varchar(255) NULL,
	lastname varchar(255) NULL,
	fullname varchar(255) NULL,
	nativename varchar(255) NULL,
	CONSTRAINT tstaff_aniliststaffid_key UNIQUE (aniliststaffid),
	CONSTRAINT tstaff_pkey PRIMARY KEY (staffid)
);

-- public.ttagcategory definition

CREATE TABLE public.ttagcategory (
	tagcategoryid serial NOT NULL,
	name varchar(255) NULL,
	CONSTRAINT ttagcategory_pkey PRIMARY KEY (categoryid)
);

-- public.tsource definition

CREATE TABLE public.tsource (
	sourceid serial NOT NULL,
	name varchar(50) NULL,
	CONSTRAINT tsource_pkey PRIMARY KEY (sourceid)
);

-- public.tstatus definition

CREATE TABLE public.tstatus (
	statusid serial NOT NULL,
	name varchar(50) NULL,
	CONSTRAINT tstatus_pkey PRIMARY KEY (statusid)
);

-- public.twatchstatus definition

CREATE TABLE public.twatchstatus (
	watchstatusid serial NOT NULL,
	name varchar(50) NULL,
	CONSTRAINT twatchstatus_pkey PRIMARY KEY (watchstatusid)
);

-- public.tnametype definition

CREATE TABLE public.tnametype (
	nametypeid serial NOT NULL,
	name varchar(50) NULL,
	CONSTRAINT tnametype_pkey PRIMARY KEY (nametypeid)
);

insert into public.tnametype (name) select 'romaji';
insert into public.tnametype (name) select 'english';
insert into public.tnametype (name) select 'native';
insert into public.tnametype (name) select 'synonyms';

-- public.tgenre definition

CREATE TABLE public.tgenre (
	genreid serial NOT NULL,
	name varchar(255) NULL,
	CONSTRAINT tgenre_pkey PRIMARY KEY (genreid)
);

-- public.tuser definition

CREATE TABLE public.tuser (
	userid serial NOT NULL,
	anilistuserid int4 NULL,
	CONSTRAINT tuser_anilistuserid_key UNIQUE (anilistuserid),
	CONSTRAINT tuser_pkey PRIMARY KEY (userid)
);

-- public.tformat definition

CREATE TABLE public.tformat (
	formatid serial NOT NULL,
	name varchar(50) NULL,
	CONSTRAINT tformat_pkey PRIMARY KEY (formatid)
);

-- public.tstudio definition

CREATE TABLE public.tstudio (
	studioid serial NOT NULL,
	aniliststudioid int4 NOT NULL,
	name varchar(50) NULL,
	CONSTRAINT tstudio_anilistsudioid_key UNIQUE (aniliststudioid),
	CONSTRAINT tstudio_pkey PRIMARY KEY (studioid)
);

-- public.tmediatype definition

CREATE TABLE public.tmediatype (
	mediatypeid serial NOT NULL,
	name varchar(50) NULL,
	CONSTRAINT tmediatype_pkey PRIMARY KEY (mediatypeid)
);

-- public.tanime definition

CREATE TABLE public.tanime (
	animeid serial NOT NULL,
	anilistanimeid int4 NULL,
	title varchar(500) NULL,
	datestart date NULL,
	dateend date NULL,
	episodes int4 NULL,
	duration int4 NULL,
	chapters int4 NULL,
	volumes	int4 NULL,
	formatid int4 NULL,
	sourceid int4 NULL,
	statusid int4 NULL,
	mediatypeid int4 NOT NULL,
	CONSTRAINT tanime_anilistanimeid_key UNIQUE (anilistanimeid),
	CONSTRAINT tanime_pkey PRIMARY KEY (animeid)
);

-- public.tanime foreign keys

ALTER TABLE public.tanime ADD CONSTRAINT fk_tanime_tformat FOREIGN KEY (formatid) REFERENCES tformat(formatid);
ALTER TABLE public.tanime ADD CONSTRAINT fk_tanime_tsource FOREIGN KEY (sourceid) REFERENCES tsource(sourceid);
ALTER TABLE public.tanime ADD CONSTRAINT fk_tanime_tstatus FOREIGN KEY (statusid) REFERENCES tstatus(statusid);
ALTER TABLE public.tanime ADD CONSTRAINT fk_tanime_tmediatype FOREIGN KEY (mediatypeid) REFERENCES tmediatype(mediatypeid);

-- public.tanime definition 
CREATE TABLE public.tanimerecs (
	userid INT NOT NULL,
	animeid INT NOT NULL,
	score INT NOT NULL,
	CONSTRAINT tanimerecs_pkey PRIMARY KEY (userid,animeid)
);

-- public.tanime foreign keys

ALTER TABLE public.tanimerecs ADD CONSTRAINT fk_tanimerecs_tanime FOREIGN KEY (animeid) REFERENCES tanime(animeid);
ALTER TABLE public.tanimerecs ADD CONSTRAINT fk_tanimerecs_tuser FOREIGN KEY (userid) REFERENCES tuser(userid);

-- public.tanimealtname definition

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

CREATE TABLE public.tanimelist (
	userid int4 NOT NULL,
	animeid int4 NOT NULL,
	datestart date NULL,
	dateend date NULL,
	score int4 NULL,
	progress int4 NULL,
	rewatched int4 NULL,
	statusid int4 NULL,
	CONSTRAINT tanimelist_pkey PRIMARY KEY (userid, animeid)
);

-- public.tanimelist foreign keys
ALTER TABLE public.tanimelist ADD CONSTRAINT fk_tanimelist_twatchstatus FOREIGN KEY (statusid) REFERENCES twatchstatus(watchstatusid);
ALTER TABLE public.tanimelist ADD CONSTRAINT fk_tanimelist_tanime FOREIGN KEY (animeid) REFERENCES tanime(animeid);
ALTER TABLE public.tanimelist ADD CONSTRAINT fk_tanimelist_tuser FOREIGN KEY (userid) REFERENCES tuser(userid);

-- public.tgenrelist definition

CREATE TABLE public.tgenrelist (
	animeid int4 NOT NULL,
	genreid int4 NOT NULL,
	CONSTRAINT tgenrelist_pkey PRIMARY KEY (animeid, genreid)
);

-- public.tgenrelist foreign keys

ALTER TABLE public.tgenrelist ADD CONSTRAINT tgenrelist_tanime FOREIGN KEY (animeid) REFERENCES tanime(animeid);
ALTER TABLE public.tgenrelist ADD CONSTRAINT tgenrelist_tgenre FOREIGN KEY (genreid) REFERENCES tgenre(genreid);

-- public.ttag definition

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

CREATE TABLE public.ttaglist (
	animeid int4 NOT NULL,
	tagid int4 NOT NULL,
	CONSTRAINT ttaglist_pkey PRIMARY KEY (animeid, tagid)
);

-- public.ttaglist foreign keys

ALTER TABLE public.ttaglist ADD CONSTRAINT fk_ttaglist_tanime FOREIGN KEY (animeid) REFERENCES tanime(animeid);
ALTER TABLE public.ttaglist ADD CONSTRAINT fk_ttaglist_ttag FOREIGN KEY (tagid) REFERENCES ttag(tagid);

-- public.tstudiolist definition

CREATE TABLE public.tstudiolist (
	animeid int4 NOT NULL,
	studioid int4 NOT NULL,
	ismain bool NOT NULL,
	CONSTRAINT tstudiolist_pkey PRIMARY KEY (animeid, studioid)
);

-- public.tstudiolist foreign keys

ALTER TABLE public.tstudiolist ADD CONSTRAINT fk_tstudiolist_tanime FOREIGN KEY (animeid) REFERENCES tanime(animeid);
ALTER TABLE public.tstudiolist ADD CONSTRAINT fk_tstudiolist_tstudio FOREIGN KEY (studioid) REFERENCES tstudio(studioid);

-- public.tstafflist definition

CREATE TABLE public.tstafflist (
	animeid int4 NOT NULL,
	staffid int4 NOT NULL,
	role varchar(255) NULL,
	CONSTRAINT tstafflist_pkey PRIMARY KEY (animeid, staffid, role)
);

-- public.tstafflist foreign keys

ALTER TABLE public.tstafflist ADD CONSTRAINT fk_tstafflist_tanime FOREIGN KEY (animeid) REFERENCES tanime(animeid);
ALTER TABLE public.tstafflist ADD CONSTRAINT fk_tstafflist_tstaff FOREIGN KEY (staffid) REFERENCES tstaff(staffid);
