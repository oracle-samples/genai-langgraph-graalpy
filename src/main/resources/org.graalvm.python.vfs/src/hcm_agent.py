# Embedding Python-based AI Agents into Java Applications version 1.0.
#
# Copyright (c)  2024,  Oracle and/or its affiliates.
# Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
from langchain_community.chat_models.oci_generative_ai import ChatOCIGenAI
from langchain_core.messages import HumanMessage

from typing import Annotated, Literal, TypedDict
from langchain_core.messages import HumanMessage
from langchain_core.tools import tool
from langgraph.graph import END, StateGraph
from langgraph.prebuilt import ToolNode,tools_condition
from langgraph.graph.message import add_messages

from java.com.oracle.ateam.examples.langgraph import RestApiClient
from java.com.oracle.ateam.examples.langgraph import PythonLogger
from java.com.oracle.ateam.examples.langgraph import Util
import oci
import base64

compartment_id = Util.getSystemProperty("compartment_id")
temperature = int(Util.getSystemProperty("temperature"))
max_tokens = int(Util.getSystemProperty("max_tokens"))
seed = int(Util.getSystemProperty("seed"))
service_endpoint = Util.getSystemProperty("service_endpoint")
model_id = Util.getSystemProperty("model_id")
hcm_host = Util.getSystemProperty("hcm_host")
hcm_username = Util.getSystemProperty("hcm_username")
secret_ocid = Util.getSystemProperty("app.vault.secret-ocid")
config_profile = Util.getSystemProperty("app.vault.config-profile")
API = Literal["Employee API", "Absences API"]

llm = ChatOCIGenAI(
        model_id=model_id,
        service_endpoint=service_endpoint,
        compartment_id=compartment_id,
        model_kwargs={"temperature": temperature, "max_tokens": max_tokens, "seed": seed}
    )

def getOCISecret():
    config = oci.config.from_file(profile_name=config_profile)
    vaultclient = oci.vault.VaultsClient(config)
    secretclient = oci.secrets.SecretsClient(config)
    secretcontents = secretclient.get_secret_bundle(secret_id=secret_ocid)
    keybase64 = secretcontents.data.secret_bundle_content.content
    keybase64bytes = keybase64.encode("ascii")
    keybytes = base64.b64decode(keybase64bytes)
    key = keybytes.decode("ascii")
    return key

@tool
def SearchAPI(
        api: Annotated[API, "Name of the API"], 
        **q: Annotated[str, '''This query parameter defines the where clause. The resource collection will be queried using the provided expressions. The value of this query parameter is one or more expressions, separate the query parameters using semicolon. Format: key=value;key=value'''],) -> str:
    """For REST API calling tasks, don't encode the URL parameters only use the functions you have been provided with. When you create the query parameters, map the fields using following fields name:\n FirstName : First Name of the Employee\nLastName : Last Name of the Employee\n personId : Person Id\n\n    Output 'DONE!' when an answer has been provided or API call did not return any results"""

    if api == "Employee API":
            endpoint = hcm_host + "/hcmRestApi/resources/11.13.18.05/emps"
    elif api == "Absences API":
            endpoint = hcm_host + "/hcmRestApi/resources/11.13.18.05/absences"
            if 'PersonId' in q:
                updated_string=q['q'].replace("PersonId", "personId")
                q['q'] = updated_string
    else:
        raise ValueError("Invalid API Call")

    restApiClient = RestApiClient()
    hcm_password = getOCISecret()
    PythonLogger.info(f"Calling Rest API : {endpoint}, parameters: {q}")
    response = restApiClient.sendGetRequest(endpoint, q, hcm_username, hcm_password)
    PythonLogger.info(f"Rest API output : {str(response)}")
    return (
        str(response)
    )

tools = [SearchAPI]
llm_with_tools = llm.bind_tools(tools)

class State(TypedDict):
    # Messages have the type "list". The `add_messages` function
    # in the annotation defines how this state key should be updated
    # (in this case, it appends messages to the list, rather than overwriting them)
    messages: Annotated[list, add_messages]

workflow = StateGraph(State)

def chatbot(state: State):
    return {"messages": [llm_with_tools.invoke(state["messages"])]}



def route_tools(
    state: State,
):
    """
    Use in the conditional_edge to route to the ToolNode if the last message
    has tool calls. Otherwise, route to the end.
    """
    if isinstance(state, list):
        ai_message = state[-1]
    elif messages := state.get("messages", []):
        ai_message = messages[-1]
    else:
        raise ValueError(f"No messages found in input state to tool_edge: {state}")
    if hasattr(ai_message, "tool_calls") and len(ai_message.tool_calls) > 0:
        return "tools"
    else:
        return END

tool_node = ToolNode(tools=tools)
workflow.add_node("chatbot", chatbot)
workflow.add_node("tools",tool_node)
workflow.add_conditional_edges(
    "chatbot",
    route_tools,
    tools_condition,
)
workflow.add_edge("tools", "chatbot")
workflow.set_entry_point("chatbot")
graph = workflow.compile()

def stream_graph_updates(user_input: str):
    for event in graph.stream({"messages": [("user", user_input)]}, {"recursion_limit": 150}):
        for value in event.values():
            return "Assistant: " + value["messages"][-1].content
        
def invoke(question):
    """Main invoke method.
    Java service will call this method to start the agent.
    """
    events = graph.stream(
    {
            "messages": [
                HumanMessage(
                    content=question
                )
            ],
        },
        # Maximum number of steps to take in the graph
        {"recursion_limit": 150},
    )
    output = ""
    for event in events:
        for value in event.values():
            output = value['messages'][-1].content
            if ( output != ""):
                PythonLogger.info("Assistant: " + output)
    return output