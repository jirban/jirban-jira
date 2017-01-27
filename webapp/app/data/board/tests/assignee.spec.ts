import {Assignee} from "../assignee";

describe('Assignee tests', ()=> {
    describe('Initials', () => {

        it('4 word name', () => {
            let assignee:Assignee = new Assignee("a", "a", "a@a.com", "http://a.com", "Kabir Middle Khan Ignored");
            expect(assignee.initials).toEqual("KMK");
        });

        it('3 word name', () => {
            let assignee:Assignee = new Assignee("a", "a", "a@a.com", "http://a.com", "Kabir Middle Khan");
            expect(assignee.initials).toEqual("KMK");
        });

        it('2 word name', () => {
            let assignee:Assignee = new Assignee("a", "a", "a@a.com", "http://a.com", "Kabir Khan");
            expect(assignee.initials).toEqual("KK");
        });

        it ('1 word name - lower case', () => {
            let assignee:Assignee = new Assignee("a", "a", "a@a.com", "http://a.com", "admin");
            expect(assignee.initials).toEqual("Adm");
        });

        it ('1 word name - upper case', () => {
            let assignee:Assignee = new Assignee("a", "a", "a@a.com", "http://a.com", "ADMIN");
            expect(assignee.initials).toEqual("Adm");
        });
    });
});