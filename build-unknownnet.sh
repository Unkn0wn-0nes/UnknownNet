# UnknownNet build script
# Unkn0wn0ne - Original Author

echo Starting build
echo Building UnknownNet-Server
cd UnknownNet-Server
mvn clean install
echo Done.
cd ..
echo Building UnknownNet-ServerTests
cd UnknownNet-ServerTests
mvn clean install
echo Done
cd ..
echo Building UnknownNet-Client
cd UnknownNet-Client
mvn clean install
cd ..
echo Done
echo Building UnknownNet-ClientTests
cd UnknownNet-ClientTests
mvn clean install
echo Done