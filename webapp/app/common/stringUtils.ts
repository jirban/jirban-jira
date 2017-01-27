export class StringUtils {
    public static makeValidKey(key:string):string{
        let validKey:string = key;
        //The forms don't like dots in the keys for the id elements
        validKey = StringUtils.replaceAll(validKey, '.', '_$_');
        if (key === validKey) {
            return key;
        }
        return validKey;
    }

    static replaceAll(str:string, tgt:string, replacement:string) : string {
        let result = str.split(tgt).join(replacement);
        if (result === str) {
            return str;
        }
        return result;
    }
}