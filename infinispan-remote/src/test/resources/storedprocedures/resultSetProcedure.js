// mode=local,language=javascript,parameters=[id,title]
var HashMap = Java.type( 'java.util.HashMap' );
var Collectors = Java.type( "java.util.stream.Collectors" );
cache.clear();
cache.put( id, title );
cache
    .entrySet()
    .stream()
    .filter( function (e) {
        return e.getKey() === id
    } )
    .limit( 1 )
    .map( function (e) {
        var hashMap = new HashMap();
        hashMap.put( 'id', e.getKey() );
        hashMap.put( 'title', e.getValue() );
        return hashMap
    } )
    .collect( Collectors.toList() );
