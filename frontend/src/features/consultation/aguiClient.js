import { HttpAgent } from "@ag-ui/client";

let sharedAgent;

function getAgent() {
    if (!sharedAgent) {
        sharedAgent = new HttpAgent({
            url: "/ag-ui/runs"
        });
    }
    return sharedAgent;
}

export function createConsultationRunInput(query) {
    return {
        messages: [
            {
                id: crypto.randomUUID(),
                role: "user",
                content: query
            }
        ]
    };
}

export async function runConsultation(query, subscriber) {
    const agent = getAgent();
    agent.setMessages([]);
    agent.setState({});
    return agent.runAgent(createConsultationRunInput(query), subscriber);
}
