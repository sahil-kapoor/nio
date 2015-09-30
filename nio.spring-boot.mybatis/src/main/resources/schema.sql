CREATE TABLE "HR"."CUSTOMERS" 
   (	"ID" NUMBER(*,0) NOT NULL ENABLE, 
	"NAME" VARCHAR2(20 BYTE) NOT NULL ENABLE, 
	"AGE" NUMBER(*,0) NOT NULL ENABLE, 
	"ADDRESS" CHAR(25 BYTE), 
	"SALARY" NUMBER(18,2), 
	 PRIMARY KEY ("ID")
  USING INDEX PCTFREE 10 INITRANS 2 MAXTRANS 255 COMPUTE STATISTICS 
  STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
  PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1 BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
  TABLESPACE "USERS"  ENABLE
   ) SEGMENT CREATION IMMEDIATE 
  PCTFREE 10 PCTUSED 40 INITRANS 1 MAXTRANS 255 NOCOMPRESS LOGGING
  STORAGE(INITIAL 65536 NEXT 1048576 MINEXTENTS 1 MAXEXTENTS 2147483645
  PCTINCREASE 0 FREELISTS 1 FREELIST GROUPS 1 BUFFER_POOL DEFAULT FLASH_CACHE DEFAULT CELL_FLASH_CACHE DEFAULT)
  TABLESPACE "USERS" ;
  /
  create or replace PROCEDURE ADD_CUSTOMER 
(
  pID IN NUMBER 
, pNAME IN VARCHAR2 
, pAGE IN NUMBER 
, pADDRESS IN VARCHAR2 
, pSALARY IN NUMBER 
) AS 
BEGIN
 INSERT INTO CUSTOMERS(
    		   ID,				  
               NAME,					  
               AGE,		
               ADDRESS,
               SALARY)
			VALUES(pID, pNAME, pAGE, pADDRESS, pSALARY);
END ADD_CUSTOMER;
/
create or replace PROCEDURE GET_CUSTOMER 
(
  pID IN OUT NUMBER 
, pNAME OUT VARCHAR2 
, pAGE OUT NUMBER 
, pADDRESS OUT VARCHAR2 
, pSALARY OUT NUMBER 
) AS 
BEGIN
  SELECT ID,				  
        NAME,					  
        AGE,		
        ADDRESS,
        SALARY
        INTO 
        pID, pNAME, pAGE, pADDRESS, pSALARY 
        FROM CUSTOMERS WHERE ID = pID; 
END GET_CUSTOMER;
/