# System architecture
```
|      |                 |      |                 |      |    
|topic1| --> helper1 --> |topic2| --> helper2 --> |topic3| --> helper3
|      |                 |      |                 |      |
```  

# Issues
## Provide consumer/producer layer in Main.scala
The result doesn't meet my expectation. Somehow `hepler1`/`hepler2`/`hepler3` has same group id and read from same topic.
The codes are in `FirstVerion` branch

## Provide consumer/producer layer in each helper
If I do it in this way, I don't have this issue.
The codes are in `ThirdVerion` branch
