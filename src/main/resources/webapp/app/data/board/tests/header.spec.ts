import {BoardHeaders, State} from "../header";
import {BoardHeaderEntry} from "../header";
import {Indexed} from "../../../common/indexed";

describe('Header tests', ()=> {

    it('No headers', () => {
        //Mock a board data with the required fields used by BoardHeaders.deserialize
        let boardData:any = {}

        let input:any = {
            states:[
                {name: "One"},
                {name: "Two"}
            ]
        };
        let headers:BoardHeaders = BoardHeaders.deserialize(boardData, input);
        expect(headers).toEqual(jasmine.anything());
        let row:BoardHeaderEntry[] = headers.topHeaders;
        expect(row.length).toEqual(2);
        checkEntry(row[0], "One", 1, 2);
        checkEntry(row[1], "Two", 1, 2);
        row = headers.bottomHeaders;
        expect(row.length).toEqual(0);

        //Also check that the states have an index
        let states:Indexed<State> = headers.boardStates;
        for (let i:number = 0 ; i < states.array.length ; i++) {
            let state:State = states.array[i];
            console.log(state.index);
            expect(state.index).toEqual(i);
        }
    });

    it('All same header', () => {
        //Mock a board data with the required fields used by BoardHeaders.deserialize
        let boardData:any = {}

        let input:any = {
            states:[
                {name: "One", header: 0},
                {name: "Two",header: 0}
            ],
            headers: ["A"]
        };
        let headers:BoardHeaders = BoardHeaders.deserialize(boardData, input);
        expect(headers).toEqual(jasmine.anything());
        let row:BoardHeaderEntry[] = headers.topHeaders;
        expect(row.length).toEqual(1);
        checkEntry(row[0], "A", 2, 1);
        row = headers.bottomHeaders;
        expect(row.length).toEqual(2);
        checkEntry(row[0], "One", 1, 1);
        checkEntry(row[1], "Two", 1, 1);
    });

    it('No header start', () => {
        //Mock a board data with the required fields used by BoardHeaders.deserialize
        let boardData:any = {}

        let input:any = {
            states:[
                {name: "One"},
                {name: "Two", header: 0},
                {name: "Three", header: 0},
                {name: "Four", header: 1}

            ],
            headers: ["A", "B"]
        };
        let headers:BoardHeaders = BoardHeaders.deserialize(boardData, input);
        expect(headers).toEqual(jasmine.anything());
        let row:BoardHeaderEntry[] = headers.topHeaders;
        expect(row.length).toEqual(3);
        checkEntry(row[0], "One", 1, 2);
        checkEntry(row[1], "A", 2, 1);
        checkEntry(row[2], "B", 1, 1);
        row = headers.bottomHeaders;
        expect(row.length).toEqual(3);
        checkEntry(row[0], "Two", 1, 1);
        checkEntry(row[1], "Three", 1, 1);
        checkEntry(row[2], "Four", 1, 1);
    });

    it('No header end', () => {
        //Mock a board data with the required fields used by BoardHeaders.deserialize
        let boardData:any = {}

        let input:any = {
            states:[
                {name: "One", header: 0},
                {name: "Two", header: 0},
                {name: "Three", header: 1},
                {name: "Four"}

            ],
            headers: ["A", "B"]
        };
        let headers:BoardHeaders = BoardHeaders.deserialize(boardData, input);
        expect(headers).toEqual(jasmine.anything());
        let row:BoardHeaderEntry[] = headers.topHeaders;
        expect(row.length).toEqual(3);
        checkEntry(row[0], "A", 2, 1);
        checkEntry(row[1], "B", 1, 1);
        checkEntry(row[2], "Four", 1, 2);
        row = headers.bottomHeaders;
        expect(row.length).toEqual(3);
        checkEntry(row[0], "One", 1, 1);
        checkEntry(row[1], "Two", 1, 1);
        checkEntry(row[2], "Three", 1, 1);
    });

    it('Mixed headers', () => {
        //Mock a board data with the required fields used by BoardHeaders.deserialize
        let boardData:any = {}

        let input:any = {
            states:[
                {name: "One"},
                {name: "Two", header: 0},
                {name: "Three", header: 0},
                {name: "Four"},
                {name: "Five", header: 1},
                {name: "Six", header: 1},
                {name: "Seven"}
            ],
            headers: ["A", "B"]
        };
        let headers:BoardHeaders = BoardHeaders.deserialize(boardData, input);
        expect(headers).toEqual(jasmine.anything());
        let row:BoardHeaderEntry[] = headers.topHeaders;
        expect(row.length).toEqual(5);
        checkEntry(row[0], "One", 1, 2);
        checkEntry(row[1], "A", 2, 1);
        checkEntry(row[2], "Four", 1, 2);
        checkEntry(row[3], "B", 2, 1);
        checkEntry(row[4], "Seven", 1, 2);
        row = headers.bottomHeaders;
        expect(row.length).toEqual(4);
        checkEntry(row[0], "Two", 1, 1);
        checkEntry(row[1], "Three", 1, 1);
        checkEntry(row[2], "Five", 1, 1);
        checkEntry(row[3], "Six", 1, 1);
    });



    function checkEntry(entry:BoardHeaderEntry, name:string, cols:number, rows:number) {
        expect(entry.name).toEqual(name);
        expect(entry.cols).toEqual(cols);
        expect(entry.rows).toEqual(rows);
    }
});