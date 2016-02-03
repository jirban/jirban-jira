System.register([], function(exports_1) {
    return {
        setters:[],
        execute: function() {
            //Tests for the BoardData component which is so central to the display of the board
            //var fs = require("fs");
            //All this can go, it is examples which seem useful for reference
            describe('BoardData sample', function () {
                it('Initial Testing', function () {
                    expect(true).toEqual(true);
                });
            });
            describe('Test JSON', function () {
                //let fileJson:any;
                //beforeEach(() => {
                //    fileJson = JSON.parse(fs.readFileSync("./boardData.json"));
                //});
                //
                //it('File JSON', () => {
                //    let o:any = JSON.parse(json);
                //    expect(o.file).toEqual(3211);
                //});
                it('Hardcode some JSON', function () {
                    var json = "\n        {\n            \"testing\": 123\n        }";
                    var o = JSON.parse(json);
                    expect(o.testing).toEqual(123);
                });
            });
            describe("A spec using beforeEach and afterEach", function () {
                var foo = 0;
                beforeEach(function () {
                    foo += 1;
                });
                afterEach(function () {
                    foo = 0;
                });
                it("is just a function, so it can contain any code", function () {
                    expect(foo).toEqual(1);
                });
                it("can have more than one expectation", function () {
                    expect(foo).toEqual(1);
                    expect(true).toEqual(true);
                });
            });
            describe("jasmine.any", function () {
                //jasmine.any takes a constructor or “class” name as an expected value. It returns true if the constructor matches the constructor of the actual value.
                it("matches any value", function () {
                    expect({}).toEqual(jasmine.any(Object));
                    expect(12).toEqual(jasmine.any(Number));
                });
                describe("when used with a spy", function () {
                    it("is useful for comparing arguments", function () {
                        var foo = jasmine.createSpy('foo');
                        foo(12, function () {
                            return true;
                        });
                        expect(foo).toHaveBeenCalledWith(jasmine.any(Number), jasmine.any(Function));
                    });
                });
            });
            describe("jasmine.anything", function () {
                //jasmine.anything returns true if the actual value is not null or undefined.
                it("matches anything", function () {
                    expect(1).toEqual(jasmine.anything());
                });
                describe("when used with a spy", function () {
                    it("is useful when the argument can be ignored", function () {
                        var foo = jasmine.createSpy('foo');
                        foo(12, function () {
                            return false;
                        });
                        expect(foo).toHaveBeenCalledWith(12, jasmine.anything());
                    });
                });
            });
        }
    }
});
//# sourceMappingURL=boardData.spec.js.map