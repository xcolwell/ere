
<?php   # Parameterised query implementation for MySQL (similar PostgreSQL's PHP function pg_query_params)
        # Example: mysql_query_params( "SELECT * FROM my_table WHERE col1=$1 AND col2=$2", array( 42, "It's ok" ) );

        if( !function_exists( 'mysql_query_params' ) ) {

                function mysql_query_params__callback( $at ) {
                        global $mysql_query_params__parameters;
                        return $mysql_query_params__parameters[ $at[1]-1 ];
                }

                function mysql_query_params( $query, $parameters=array(), $database=false ) {

                        // Escape parameters as required & build parameters for callback function
                        global $mysql_query_params__parameters;
                        foreach( $parameters as $k=>$v )
                                $parameters[$k] = ( is_int( $v ) ? $v : ( NULL===$v ? 'NULL' : "'".mysql_real_escape_string( $v )."'" ) );
                        $mysql_query_params__parameters = $parameters;

                        // Call using mysql_query
                        $sql = preg_replace_callback( '/\$([0-9]+)/', 'mysql_query_params__callback', $query );
                        #echo $sql;
                        if( false===$database )
                                return mysql_query( $sql );
                        else    return mysql_query( $sql, $database );

                }
        }

?>