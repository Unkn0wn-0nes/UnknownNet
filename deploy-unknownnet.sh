# UnknownNet build script
# Unkn0wn0ne - Original Author

echo Starting build + deployment.

echo Building UnknownNet-Server
cd UnknownNet-Server
mvn clean deploy 
echo Done.
cd ..
echo Building UnknownNet-ServerTests
cd UnknownNet-ServerTests
mvn clean deploy
echo Done
cd ..
echo Building UnknownNet-Client
cd UnknownNet-Client
mvn clean deploy
cd ..
echo Done
echo Building UnknownNet-ClientTests
cd UnknownNet-ClientTests
mvn clean deploy
echo Done