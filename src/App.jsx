import { useEffect, useRef, useState } from "react";

import ChartMessage from "./components/ChartMessage.jsx";



import {

  BrainCircuit, Plus, MessageSquare, FileText, Paperclip, Send,

  Trash2, X, Square, Menu, Pencil, LoaderCircle, Sparkles,

  AlertCircle, PanelLeftClose, Workflow, Image as ImageIcon, BarChart3, ShieldCheck, Zap, ArrowUpRight, Globe2, Database, Cpu, Activity, LockKeyhole, ChevronRight

} from "lucide-react";

import { api } from "./api.jsx";

import "./App.css";
import "./chart.css";
import "./futuristic.css";



function getMode(conversation) {

  return String(conversation?.chatMode || "GENERAL").toUpperCase();

}



function shortName(value, limit = 30) {

  const name = String(value || "");

  return name.length > limit ? `${name.slice(0, limit - 3)}...` : name;

}



const DOCUMENT_EXTENSIONS = /\.(pdf|txt|csv|xlsx|xls|json)$/i;

const IMAGE_EXTENSIONS = /\.(png|jpe?g|webp)$/i;



export default function App() {

  const [conversations, setConversations] = useState([]);

  const [activeId, setActiveId] = useState(null);

  const [messages, setMessages] = useState([]);

  const [documents, setDocuments] = useState([]);

  const [input, setInput] = useState("");

  const [agentMode, setAgentMode] = useState(false);

  const [agentDocument, setAgentDocument] = useState(null);

  const [agentSecondDocument, setAgentSecondDocument] = useState(null);

  const [agentImage, setAgentImage] = useState(null);

  const [xColumn, setXColumn] = useState("");

  const [yColumn, setYColumn] = useState("");

  const [chartType, setChartType] = useState("bar");

  const [aggregation, setAggregation] = useState("none");

  const [busy, setBusy] = useState(false);

  const [uploading, setUploading] = useState(false);

  const [starting, setStarting] = useState(false);

  const [loadingHistory, setLoadingHistory] = useState(false);

  const [error, setError] = useState("");

  const [sidebar, setSidebar] = useState(true);



  const fileInputRef = useRef(null);

  const agentFileRef = useRef(null);

  const bottomRef = useRef(null);

  const abortRef = useRef(null);

  const activeRef = useRef(null);

  const requestRef = useRef(0);

  const runRef = useRef(0);



  const active = conversations.find((c) => c.id === activeId);

  const documentMode = getMode(active) === "DOCUMENT";

  const locked = busy || uploading || starting || loadingHistory;



  function clearAgentFiles() {

    setAgentDocument(null);

    setAgentSecondDocument(null);

    setAgentImage(null);

  }



  function addAgentFiles(event) {

    const files = Array.from(event.target.files || []);

    event.target.value = "";

    if (!files.length) return;



    let first = agentDocument;

    let second = agentSecondDocument;

    let image = agentImage;



    for (const file of files) {

      if (IMAGE_EXTENSIONS.test(file.name)) {

        image = file;

      } else if (DOCUMENT_EXTENSIONS.test(file.name)) {

        if (!first) first = file;

        else if (!second) second = file;

        else {

          setError("You can attach at most two documents per agent request.");

          return;

        }

      } else {

        setError(`Unsupported file: ${file.name}`);

        return;

      }

    }

    setAgentDocument(first);

    setAgentSecondDocument(second);

    setAgentImage(image);

    setError("");

  }



  useEffect(() => {

    bottomRef.current?.scrollIntoView({ behavior: "smooth" });

  }, [messages, busy]);



  useEffect(() => {

    let mounted = true;

    api.conversations()

      .then((data) => {

        if (mounted) setConversations(Array.isArray(data) ? data : []);

      })

      .catch((err) => {

        if (mounted) setError(err.message);

      });

    return () => { mounted = false; };

  }, []);



  async function selectConversation(id) {

    if (locked) return;

    const requestId = ++requestRef.current;

    activeRef.current = id;

    setActiveId(id);

    setMessages([]);

    setDocuments([]);

    clearAgentFiles();

    setError("");

    setLoadingHistory(true);

    try {

      const [history, docs] = await Promise.all([api.messages(id), api.documents(id)]);

      if (requestId !== requestRef.current || activeRef.current !== id) return;

      setMessages(Array.isArray(history) ? history : []);

      setDocuments(Array.isArray(docs) ? docs : []);

    } catch (err) {

      if (requestId === requestRef.current) setError(err.message);

    } finally {

      if (requestId === requestRef.current) setLoadingHistory(false);

    }

  }



  async function createConversation(mode) {

    if (locked) return;

    setStarting(true);

    setError("");

    try {

      const title = mode === "DOCUMENT" ? "New Document Chat" : "New Chat";

      const created = await api.create(mode, title);

      ++requestRef.current;

      setConversations((prev) => [created, ...prev]);

      activeRef.current = created.id;

      setActiveId(created.id);

      setMessages([]);

      setDocuments([]);

      clearAgentFiles();

      setInput("");

      setLoadingHistory(false);

    } catch (err) {

      setError(err.message);

    } finally {

      setStarting(false);

    }

  }



  async function deleteConversation(id) {

    if (locked) return;

    if (!window.confirm("Delete this conversation and its messages? Uploaded PDFs will be kept.")) return;

    setError("");

    try {

      await api.delete(id);

      setConversations((prev) => prev.filter((c) => c.id !== id));

      if (activeRef.current === id) {

        ++requestRef.current;

        activeRef.current = null;

        setActiveId(null);

        setMessages([]);

        setDocuments([]);

        clearAgentFiles();

        setInput("");

      }

    } catch (err) {

      setError(err.message);

    }

  }



  async function renameConversation(id) {

    if (locked) return;

    const current = conversations.find((c) => c.id === id);

    const title = window.prompt("Enter conversation title:", current?.title || "New Chat");

    if (!title?.trim()) return;

    setError("");

    try {

      const updated = await api.rename(id, title.trim());

      setConversations((prev) => prev.map((c) => c.id === id ? updated : c));

    } catch (err) {

      setError(err.message);

    }

  }



  async function uploadFiles(event) {

    const files = Array.from(event.target.files || []);

    event.target.value = "";

    if (!files.length || !active || !documentMode || locked) return;

    const conversationId = active.id;

    setUploading(true);

    setError("");

    try {

      for (const file of files) {

        if (!/\.pdf$/i.test(file.name)) throw new Error(`${file.name}: only PDFs are supported.`);

        const uploaded = await api.upload(file);

        if (!uploaded?.id) throw new Error(`No document ID returned for ${file.name}`);

        await api.attach(conversationId, uploaded.id);

      }

    } catch (err) {

      setError(`PDF upload failed: ${err.message}`);

    } finally {

      try {

        const refreshed = await api.documents(conversationId);

        if (activeRef.current === conversationId) {

          setDocuments(Array.isArray(refreshed) ? refreshed : []);

        }

      } catch (err) {

        setError((previous) => previous || err.message);

      }

      setUploading(false);

    }

  }



  async function detachDocument(documentId) {

    if (!active || locked) return;

    const conversationId = active.id;

    setError("");

    try {

      await api.detach(conversationId, documentId);

      if (activeRef.current === conversationId) {

        setDocuments((prev) => prev.filter((doc) => doc.id !== documentId));

      }

    } catch (err) {

      setError(err.message);

    }

  }



  async function send(event) {

    event?.preventDefault();

    const question = input.trim();

    if (!question || locked || !active) return;

    if (!agentMode && documentMode && documents.length === 0) {

      setError("Attach at least one PDF before asking a document question.");

      return;

    }



    const conversationId = active.id;

    const initialTitle = active.title;

    const firstMessage = messages.length === 0;

    const runId = ++runRef.current;

    const assistantTempId = `assistant-${runId}`;

    const controller = new AbortController();

    abortRef.current = controller;

    setBusy(true);

    setError("");

    setInput("");

    setMessages((prev) => [

      ...prev,

      { id: `user-${runId}`, role: "USER", content: question, sources: [] },

      { id: assistantTempId, role: "ASSISTANT", content: "", sources: [], streaming: true }

    ]);



    try {

      const onChunk = (chunk) => {

        if (activeRef.current !== conversationId) return;

        setMessages((prev) => prev.map((message) =>

          message.id === assistantTempId

            ? { ...message, content: message.content + chunk }

            : message

        ));

      };



      if (agentMode) {

        await api.agentStream(

          conversationId,

          {

            message: question,

            document: agentDocument,

            secondDocument: agentSecondDocument,

            image: agentImage,

            xColumn: xColumn.trim(),

            yColumn: yColumn.trim(),

            chartType,

            aggregation

          },

          controller.signal,

          onChunk

        );

        clearAgentFiles();

      } else {

        await api.stream(conversationId, question, controller.signal, onChunk);

      }



      try {

        const history = await api.messages(conversationId);

        if (activeRef.current === conversationId && Array.isArray(history)) {

          setMessages(history);

        }

      } catch (err) {

        // Keep the streamed answer visible if refreshing history fails.

        setMessages((prev) => prev.map((m) => m.id === assistantTempId ? { ...m, streaming: false } : m));

        setError(`Response received, but history could not refresh: ${err.message}`);

      }



      if (firstMessage && ["New Chat", "New Document Chat"].includes(initialTitle)) {

        try {

          const updated = await api.rename(conversationId, shortName(question, 45));

          setConversations((prev) => prev.map((c) => c.id === conversationId ? updated : c));

        } catch {

          // Auto-renaming is optional.

        }

      }

    } catch (err) {

      setError(err.name === "AbortError"

        ? "Generation stopped. Partial responses may not be saved."

        : err.message);

      setMessages((prev) => prev.map((m) => m.id === assistantTempId ? { ...m, streaming: false } : m));

      try {

        const history = await api.messages(conversationId);

        if (activeRef.current === conversationId && Array.isArray(history) && history.length) {

          // Keep unsaved partial responses visible when the request was aborted.

          if (err.name !== "AbortError") setMessages(history);

        }

      } catch {

        // Preserve the UI and original error.

      }

    } finally {

      abortRef.current = null;

      setBusy(false);

    }

  }



  return (

    <div className="shell futuristic-shell">
      <div className="ambient ambient-one" aria-hidden="true" />
      <div className="ambient ambient-two" aria-hidden="true" />

      <aside className={`sidebar ${sidebar ? "" : "collapsed"}`}>

        <div className="brand">

          <span className="brand-mark"><BrainCircuit size={23} /></span>
          <span className="brand-copy"><strong>LocalMind <span>AI</span></strong><small>INTELLIGENCE STUDIO</small></span>

          <button type="button" className="icon-button sidebar-toggle" onClick={() => setSidebar(false)} title="Hide sidebar">

            <PanelLeftClose size={18} />

          </button>

        </div>

        <div className="sidebar-eyebrow">WORKSPACE <span>01</span></div>
        <div className="new-actions">

          <button type="button" disabled={locked} onClick={() => createConversation("GENERAL")}>

            <Plus size={17} /> New Chat

          </button>

          <button type="button" disabled={locked} onClick={() => createConversation("DOCUMENT")}>

            <FileText size={17} /> Document Chat

          </button>

        </div>

        <div className="section-label">CONVERSATIONS</div>

        <div className="conversation-list">

          {conversations.map((conversation) => (

            <div key={conversation.id} className={`conversation ${activeId === conversation.id ? "selected" : ""}`}>

              <button type="button" className="conversation-select" disabled={locked}

                title={conversation.title} onClick={() => selectConversation(conversation.id)}>

                {getMode(conversation) === "DOCUMENT" ? <FileText size={16} /> : <MessageSquare size={16} />}

                <span>{conversation.title}</span>

              </button>

              <button type="button" className="icon-button small" title="Rename conversation" disabled={locked}

                onClick={() => renameConversation(conversation.id)}><Pencil size={14} /></button>

              <button type="button" className="icon-button small danger" title="Delete conversation" disabled={locked}

                onClick={() => deleteConversation(conversation.id)}><Trash2 size={14} /></button>

            </div>

          ))}

          {conversations.length === 0 && <p className="muted small-note">No conversations yet.</p>}

        </div>

        <div className="sidebar-footer"><div className="sidebar-security"><ShieldCheck size={17}/><span><strong>Privacy by design</strong><small>Local-first workspace</small></span></div><div className="sidebar-runtime"><span className="status-dot" /> Ollama · Local inference</div></div>

      </aside>



      <main className="main">

        <header className="topbar">

          {!sidebar && <button type="button" className="icon-button" title="Show sidebar" onClick={() => setSidebar(true)}><Menu size={21} /></button>}

          <div className="header-title">

            <strong>{active?.title || "LocalMind AI"}</strong>

            <span>{active ? (agentMode ? "AI Agent · Multi-tool workspace" : documentMode ? "Document Chat · Multi-PDF RAG" : "General Chat") : "Your private AI assistant"}</span>

          </div>

          <div className="top-right"><span className="topbar-private"><LockKeyhole size={13}/> PRIVATE WORKSPACE</span><span className="model-pill"><span className="status-dot" /> Local AI</span></div>

        </header>



        {error && (

          <div className="error-banner">

            <AlertCircle size={17} /><span>{error}</span>

            <button type="button" className="icon-button" title="Dismiss error" onClick={() => setError("")}><X size={16} /></button>

          </div>

        )}



        {active && (

          <div className="documents-bar" style={{ flexWrap: "wrap", gap: 10 }}>

            <button type="button" className={`agent-toggle ${agentMode ? "active" : ""}`}

              disabled={locked} onClick={() => setAgentMode((value) => !value)}>

              <Workflow size={16} /> {agentMode ? "AI Agent: On" : "Enable AI Agent"}

            </button>

            {agentMode && (

              <>

                <button type="button" className="attach-button" disabled={locked}

                  onClick={() => agentFileRef.current?.click()}><Paperclip size={15} /> Add agent files</button>

                <input ref={agentFileRef} hidden type="file" multiple

                  accept=".pdf,.txt,.csv,.xlsx,.xls,.json,.png,.jpg,.jpeg,.webp"

                  onChange={addAgentFiles} />

                {[agentDocument, agentSecondDocument, agentImage].map((file, index) => file && (

                  <span className="document-chip" key={`${index}-${file.name}`} title={file.name}>

                    {index === 2 ? <ImageIcon size={14} /> : <FileText size={14} />}

                    {shortName(file.name, 24)}

                    <button type="button" title="Remove file" disabled={locked}

                      onClick={() => index === 0 ? setAgentDocument(null) : index === 1 ? setAgentSecondDocument(null) : setAgentImage(null)}>

                      <X size={13} />

                    </button>

                  </span>

                ))}

                <span className="documents-label"><BarChart3 size={14} /> Chart options</span>

                <input aria-label="X-axis column" placeholder="X column" value={xColumn}

                  onChange={(e) => setXColumn(e.target.value)} style={{ width: 110, padding: 7 }} disabled={locked} />

                <input aria-label="Y-axis column" placeholder="Y column" value={yColumn}

                  onChange={(e) => setYColumn(e.target.value)} style={{ width: 110, padding: 7 }} disabled={locked} />

                <select aria-label="Chart type" value={chartType} disabled={locked} onChange={(e) => setChartType(e.target.value)}>

                  <option value="bar">Bar</option><option value="line">Line</option>

                  <option value="pie">Pie</option><option value="scatter">Scatter</option>

                </select>

                <select aria-label="Aggregation" value={aggregation} disabled={locked} onChange={(e) => setAggregation(e.target.value)}>

                  <option value="none">None</option><option value="sum">Sum</option>

                  <option value="average">Average</option><option value="count">Count</option>

                </select>

              </>

            )}

          </div>

        )}



        {active && documentMode && (

          <div className="documents-bar">

            <span className="documents-label"><FileText size={15} /> Attached PDFs</span>

            {documents.map((document) => (

              <span className="document-chip" key={document.id} title={document.fileName}>

                <FileText size={14} /> {shortName(document.fileName)}

                <button type="button" title="Detach PDF" disabled={locked} onClick={() => detachDocument(document.id)}><X size={13} /></button>

              </span>

            ))}

            <button type="button" className="attach-button" disabled={locked} onClick={() => fileInputRef.current?.click()}>

              {uploading ? <LoaderCircle size={15} className="spin" /> : <Paperclip size={15} />}

              {uploading ? "Processing PDF..." : "Add PDFs"}

            </button>

            <input ref={fileInputRef} hidden type="file" accept=".pdf,application/pdf" multiple onChange={uploadFiles} />

          </div>

        )}



        <section className="chat-scroll">

          {!active ? (
            <div className="welcome hero-welcome">
              <div className="hero-overline"><span className="hero-overline-dot"/> YOUR PRIVATE AI OPERATING SYSTEM <span className="hero-overline-dot"/></div>
              <div className="welcome-icon hero-orb"><BrainCircuit size={45}/></div>
              <div className="hero-kicker"><Sparkles size={14}/> THE NEXT ERA OF LOCAL INTELLIGENCE</div>
              <h1>Think beyond <em>the cloud.</em></h1>
              <p>One intelligent workspace for reasoning, research, documents, and agent-powered workflows. Your ideas stay in your hands.</p>
              <div className="welcome-actions hero-actions">
                <button type="button" disabled={locked} onClick={() => createConversation("GENERAL")}><Zap size={18}/> Launch AI Chat <ArrowUpRight size={16}/></button>
                <button type="button" disabled={locked} onClick={() => createConversation("DOCUMENT")}><FileText size={18}/> Explore Documents <ChevronRight size={16}/></button>
              </div>
              <div className="capability-heading"><span>EXPLORE YOUR WORKSPACE</span><span>BUILT FOR WHAT'S NEXT</span></div>
              <div className="capability-grid">
                <button type="button" disabled={locked} onClick={() => createConversation("GENERAL")} className="capability-card"><span className="cap-icon violet"><MessageSquare size={22}/></span><strong>AI Conversations</strong><small>Context-aware streaming chat</small><ArrowUpRight size={16} className="cap-arrow"/></button>
                <button type="button" disabled={locked} onClick={() => createConversation("DOCUMENT")} className="capability-card"><span className="cap-icon cyan"><FileText size={22}/></span><strong>Document Intelligence</strong><small>Multi-PDF retrieval & citations</small><ArrowUpRight size={16} className="cap-arrow"/></button>
                <button type="button" disabled={locked} onClick={() => createConversation("GENERAL")} className="capability-card"><span className="cap-icon pink"><Workflow size={22}/></span><strong>Agent Workflows</strong><small>Activate agents inside chat</small><ArrowUpRight size={16} className="cap-arrow"/></button>
                <button type="button" disabled={locked} onClick={() => createConversation("GENERAL")} className="capability-card"><span className="cap-icon amber"><BarChart3 size={22}/></span><strong>Data Visualization</strong><small>Interactive, evidence-led charts</small><ArrowUpRight size={16} className="cap-arrow"/></button>
              </div>
              <div className="hero-footnotes"><span><ShieldCheck size={14}/> Privacy-first</span><span><Cpu size={14}/> Powered by Ollama</span><span><Database size={14}/> Document RAG</span></div>
            </div>
          ) : loadingHistory ? (

            <div className="welcome"><LoaderCircle size={32} className="spin" /><p>Loading conversation...</p></div>

          ) : messages.length === 0 ? (

            <div className="welcome">

              <div className="welcome-icon"><Sparkles size={37} /></div>

              <h1>{agentMode ? "What should the agent work on?" : documentMode ? "Ask your documents anything." : "What can I help with?"}</h1>

              <p>{agentMode ? "Attach files to analyze or compare, or ask a question using local AI tools." : documentMode ? "Attach PDFs above, then ask questions about their contents." : "Your conversations run through local Spring Boot and Ollama."}</p>
              <div className="prompt-grid">
                {[
                  ["Explain a concept", "Explain retrieval-augmented generation with a practical example"],
                  ["Plan a project", "Help me plan an AI hackathon project with milestones"],
                  ["Analyze data", "Help me analyze a dataset and create an insightful chart"],
                  ["Research a topic", "Explain how local AI agents use tools safely"]
                ].map(([label, prompt]) => <button type="button" key={label} disabled={locked} onClick={() => setInput(prompt)}><Sparkles size={14}/>{label}<ArrowUpRight size={14}/></button>)}
              </div>

            </div>

          ) : (

            <div className="messages">

              {messages.map((message) => {

                const isUser = String(message.role).toUpperCase() === "USER";

                return (

                  <div key={message.id} className={`message-row ${isUser ? "from-user" : "from-ai"}`}>

                    {!isUser && <div className="avatar"><BrainCircuit size={18} /></div>}

                    <div className="message-body">

                      {isUser ? (

                        <div className="user-bubble">{message.content}</div>

                      ) : (

                        <div className="markdown">

                          <ChartMessage content={message.content || (message.streaming ? "Thinking…" : "")} streaming={Boolean(message.streaming)} />

                          {message.streaming && <span className="cursor" />}

                        </div>

                      )}

                      {Array.isArray(message.sources) && message.sources.length > 0 && (

                        <div className="sources">

                          <span>Sources</span>

                          {message.sources.map((source, index) => (

                            <span className="source" key={source.id ?? `${source.fileName}-${source.pageNumber}-${index}`}

                              title={`Chunk ${source.chunkIndex ?? "unknown"}`}>

                              <FileText size={12} /> {source.fileName} · p. {source.pageNumber}

                            </span>

                          ))}

                        </div>

                      )}

                    </div>

                  </div>

                );

              })}

              <div ref={bottomRef} />

            </div>

          )}

        </section>



        <div className="composer-wrap">

          <form className="composer" onSubmit={send}>

            <textarea rows={2} value={input} disabled={!active || locked}

              placeholder={!active ? "Start a conversation first..." : agentMode ? "Ask the agent to analyze, compare, or explain..." : documentMode ? "Ask about your attached PDFs..." : "Message LocalMind AI..."}

              onChange={(event) => setInput(event.target.value)}

              onKeyDown={(event) => {

                if (event.key === "Enter" && !event.shiftKey && !event.nativeEvent.isComposing) {

                  event.preventDefault();

                  send(event);

                }

              }} />

            <div className="composer-bottom">

              <span>{agentMode ? "Agent tools · Local Ollama" : documentMode ? `${documents.length} PDF${documents.length === 1 ? "" : "s"} attached` : "Local inference · Ollama"}</span>

              {busy ? (

                <button type="button" className="send-button stop" title="Stop generation" onClick={() => abortRef.current?.abort()}><Square size={17} /></button>

              ) : (

                <button type="submit" className="send-button" disabled={!active || !input.trim() || locked} title="Send message"><Send size={18} /></button>

              )}

            </div>

          </form>

          <p className="footer-note">LocalMind AI can make mistakes. Verify important answers and citations.</p>

        </div>

      </main>

    </div>

  );

}
