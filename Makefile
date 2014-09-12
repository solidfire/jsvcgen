test : src/jsvcgen/jsvc-generate
	PYTHONPATH=./src/jsvcgen/jsvcgen ./src/jsvcgen/jsvc-generate test/jsonwsp/simple.json build

clean :
	rm -rf build
