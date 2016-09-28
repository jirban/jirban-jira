import {Indexed} from "../../common/indexed";
import {Projects} from "./project";
export class ParallelTask {
    private _name:string;
    private _code:string;
    private _options:Indexed<string>;

    constructor(name: string, code: string, options:Indexed<string>) {
        this._name = name;
        this._code = code;
        this._options = options;
    }

    get name(): string {
        return this._name;
    }

    get code(): string {
        return this._code;
    }

    get options(): Indexed<string> {
        return this._options;
    }

    getOptionForIndex(optionIndex:number):string {
        return this._options.forIndex(optionIndex);
    }

    static deserialize(input:any):ParallelTask {
        let options:Indexed<string> = new Indexed<string>();
        options.indexArray(
            input["options"],
            (entry) => {
                return entry;
            },
            (value) => {
                return value;
            }
        );

        return new ParallelTask(input["name"], input["display"], options);
    }
}

export class ParallelTaskDeserializer {
    deserialize(input:any, projects:Projects):Indexed<ParallelTask> {

        let optionsByParallelTaskCode:Indexed<ParallelTask>  = new Indexed<ParallelTask>();

        for (let project of projects.boardProjects.array) {
            if (!project.parallelTasks) {
                continue;
            }
            for (let task of project.parallelTasks.array) {
                let uberTask:ParallelTask = optionsByParallelTaskCode.forKey(task.code);

                for (let option of task.options.array) {
                    if (!uberTask) {
                        let options:Indexed<string> = new Indexed<string>();
                        options.indexArray(
                            task.options.array,
                            (entry) => {
                                return entry;
                            },
                            (value) => {
                                return value;
                            }
                        );
                        uberTask = new ParallelTask(task.name, task.code, options);
                        optionsByParallelTaskCode.add(task.code, uberTask);
                        break;
                    }
                    if (!uberTask.options.forKey(option)) {
                        uberTask.options.add(option, option);
                    }
                }
            }
        }
        return optionsByParallelTaskCode;
    }
}

