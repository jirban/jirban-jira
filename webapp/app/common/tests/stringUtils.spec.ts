import {StringUtils} from "../stringUtils";
describe('StringUtils tests', () => {

    it('Simple no replacement', () => {
        let input:string = "abc";
        let result:string = StringUtils.makeValidKey(input);
        expect(result).toBe(input);
    });

    it('Space no replacement', () => {
        let input:string = "abc def";
        let result:string = StringUtils.makeValidKey(input);
        expect(result).toBe(input);
    });

    it('Replace single dot', () => {
        let input:string = "abc.def";
        let result:string = StringUtils.makeValidKey(input);
        expect(result).not.toBe(input);
        expect(result).toEqual("abc_$_def");
    });

    it('Replace several dots', () => {
        let input:string = "abc.def.ghi";
        let result:string = StringUtils.makeValidKey(input);
        expect(result).not.toBe(input);
        expect(result).toEqual("abc_$_def_$_ghi");
    });
});
