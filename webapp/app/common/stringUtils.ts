export class StringUtils {
    public static makeValidKey(key:string):string{
        let validKey:string = key;
        let index:number = validKey.indexOf('.', 0);
        while (index >= 0) {
            //The forms don't like dots in the keys
            validKey = validKey.replace('.', '_$_');
            index = validKey.indexOf('.', index);
        }
        return validKey;
    }
}