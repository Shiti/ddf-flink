# SQL Support in ddf-flink

The SQL support in ddf-flink is limited to the following statements/syntax
Note: The table and column names are case-sensitive

## CREATE

```sql
CREATE TABLE person (first_name VARCHAR, last_name VARCHAR, age INTEGER, expYear INT, married BOOLEAN)
```

The supported column types are:

1. STRING / VARCHAR
2. INTEGER / INT
3. LONG / BIGINT
4. FLOAT
5. DOUBLE
6. DATE/TIMESTAMP
7. BOOLEAN / BOOL

## LOAD DATA

```sql
LOAD 'file:///usrs/juin/io/persons' delimited by ' '  into person 

LOAD 'file:///usrs/juin/io/persons' delimited by ' ' WITH NULL 'NA' NO DEFAULTS into person  
```

Note: The file path should be absolute(including the file system)

Specifying the delimiter is optional and if not specified it is assumed to be `,`.
By default, `null` values are replaced by default value for that column type.
If you do not wish to use the default value, use statement similar to second example above

The default values used are:

| datatype         | default value |
|------------------|---------------|
| STRING / VARCHAR | ""            |
| INTEGER / INT    | 0             |
| LONG / BIGINT    | 0             |
| FLOAT            | 0.0           |
| DOUBLE           | 0.0           |
| DATE / TIMESTAMP | new Date()    |
| BOOLEAN / BOOL   | false         |

## SELECT

```sql
SELECT * FROM person

SELECT Year,Month FROM airline

SELECT Year,Month FROM airline WHERE Year > 2008 AND Month > 1

SELECT Year,Month FROM airline LEFT JOIN year_names on (Year = Year_num)

SELECT Year,Month FROM airline ORDER BY Year DESC

SELECT Year,Month,COUNT(Cancelled) AS NUM_CANCELLED FROM airline GROUP BY Year,Month ORDER BY Year DESC LIMIT 5
```

The functions `MIN()`, `MAX()`, `SUM()`, `AVG()` and `COUNT()` can be called on individual columns. But, when using a function, aliases are compulsory.

The following joins can be used:

1. INNER
2. LEFT SEMI
3. LEFT OUTER
4. RIGHT OUTER
5. FULL OUTER