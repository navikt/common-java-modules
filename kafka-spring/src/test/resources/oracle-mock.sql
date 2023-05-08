-- H2 does not have DBMS_LOB.COMPARE so we need to provide a mock
CREATE SCHEMA DBMS_LOB;
    CREATE ALIAS DBMS_LOB.COMPARE AS '
    int compare(String lob_1, String lob_2) {
        return lob_1.equals(lob_2) ? 0 : -1;
    }
';