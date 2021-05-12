# skript-db

 > Sensible SQL support for Skript.
---

### Difference from original skript-db
- Fixed local variables disappearance in newer Skript versions (very hacky fix, but it works, so that's good!)
- Thread-pool size is now automatically increasing on demand to use of CachedThreadPool, instead of a fixed hard-coded number
- Uses a newer version of HikariCP
- Only meant to be used by newer Minecraft versions (1.8 is not supported)

### Expression `Data Source` => `datasource`
Stores the connection information for a data source. This should be saved to a variable in a
 `script load` event or manually through an effect command.

 The url format for your database may vary! The example provided uses a MySQL database.
#### Syntax
```
[the] data(base|[ ]source) [(of|at)] %string%
```

#### Examples
```
set {sql} to the database "mysql://localhost:3306/mydatabase?user=admin&password=12345&useSSL=false"
```

---

### Effect `Execute Statement`
Executes a statement on a database and optionally stores the result in a variable. Expressions
 embedded in the query will be escaped to avoid SQL injection.

 If a single variable, such as `{test}`, is passed, the variable will be set to the number of
 affected rows.

 If a list variable, such as `{test::*}`, is passed, the query result will be mapped to the list
 variable in the form `{test::<column name>::<row number>}`
 
 If `quickly` is specified, the SQL query will be done without jumping back to main thread, which speeds it up by 50ms, however that makes code after it to also be on separate thread, you can jump back to main thread by adding `wait a tick`
#### Syntax
```
[quickly] execute %string% (in|on) %datasource% [and store [[the] (output|result)[s]] (to|in) [the] [var[iable]] %-objects%]
```

#### Examples
```
execute "select * from table" in {sql} and store the result in {output::*}
```
```
execute "select * from %{table variable}%" in {sql} and store the result in {output::*}
```

---

### Expression `Last Data Source Error` => `text`
Stores the error from the last executed statement, if there was one.
#### Syntax
```
[the] [last] (sql|db|data(base|[ ]source)) error
```

---

### Expression `Unsafe Expression` => `text`
Opts out of automatic SQL injection protection for a specific expression in a statement.
#### Syntax
```
unsafe %text%
```

#### Examples
```
execute "select %unsafe {columns variable}% from %{table variable}%" in {sql}
```
```
execute unsafe {fully dynamic query} in {sql}
```

---
