
return a set of links

# TODO: this should do a BFS given some initial wids

# fetches all links connected to any of the givens ids
# as either parent or child

// pass in include and exclude strings, formatted as sql

select parent_wid, child_wid from links where (parent_wid in (108) or child_wid in (108)) and 
not (parent_wid in (100) or child_wid in (100))


limits:
max size
max depth
or both

current fringe
union of all previous fringes



also want a php to
return ids for given offsets,
exlcuding a set of given ids




expansion algorithm is:

1. expand by max size, max depth
2. if we need more,
   random sample N
   and expand those by remaining size
3. if we need more, keep random sampling and expanding based on some ratio





- 32mb video memory
  how many vbos can we store?
  
  i.e. what's the expected size of a vbo?
  
  can we store all blobs in video memory?
  
  we should prob. actively render the countours, since
  they are going to be variable width/size
  (always shifting)
  
  


