import React, {useEffect, useState} from 'react';
import Modal from './Modal';
import Cookies from 'js-cookie';
import './App.css';

function App() {

    const [inputMessage, setInputMessage] = useState('');
    const [ongoingConversation, setOngoingConversation] = useState([]); // Track the ongoing conversation's messages
    const [conversations, setConversations] = useState([]); // Track the saved conversations
    const [currentConversationId, setCurrentConversationId] = useState(null); // Track the current conversation's ID
    const [isLoading, setIsLoading] = useState(false);
    const [errorMessage, setErrorMessage] = useState('');
    const [showError, setShowError] = useState(false);
    const [infoMessage, setInfoMessage] = useState('');
    const [showInfo, setShowInfo] = useState(false);
    const [abortController, setAbortController] = useState(null);

    const [showModal, setShowModal] = useState(false);
    const [isLoginModal, setIsLoginModal] = useState(true); // True for login, false for register

    const [isAuthenticated, setIsAuthenticated] = useState(false);
    const [username, setUsername] = useState('');

    let API_URL = 'https://simons-gpt.azurewebsites.net';
    // let API_URL = 'http://localhost:8080';

    const handleInputChange = (event) => {
        setInputMessage(event.target.value);
    };

    const sendMessage = async () => {
        if (inputMessage.trim()) {
            const controller = new AbortController();
            setAbortController(controller);
            setIsLoading(true); // Start loading
            setErrorMessage('');
            setShowError(false);
            setInfoMessage('');
            setShowInfo(false);

            // if this is the first message of a new conversation, create one in the backend
            if (isAuthenticated && !currentConversationId) {
                try {
                    const conversation = await createConversation();
                    await fetchConversations();
                    setCurrentConversationId(conversation.conversationId);
                } catch (error) {
                    console.error(error);
                    setErrorMessage(`创建对话记录失败，本条对话将不会被保存`);
                    setShowError(true);
                }
            }
            // Add the user message to the conversation
            setOngoingConversation(prevState => [...prevState, { role: 'user', content: inputMessage }]);
            setInputMessage(''); // Clear the input field
        }
    };

    // Render messages with different alignment based on sender
    const renderMessages = () => {
        return ongoingConversation.map((msg, index) => (
            <div
                key={index}
                className={`message ${msg.role === 'user' ? 'user-message' : 'assistant-message'}`}>
                {msg.content.replaceAll(/\n\n(?!\n)/g, '')}
            </div>
        ));
    };

    const interruptProcess = () => {
        if (abortController) {
            abortController.abort();
            setAbortController(null);
        }
        setIsLoading(false);
    }

    useEffect(() => {
        if (ongoingConversation.length > 0 && ongoingConversation[ongoingConversation.length - 1].role === 'user') {
            const jwtToken = Cookies.get('jwtToken');
            const headers = {
                'Content-Type': 'application/json',
            };

            if (jwtToken) {
                headers['Authorization'] = `Bearer ${jwtToken}`;
            }

            const body = {
                conversationId: currentConversationId,
                chatMessages: ongoingConversation.flatMap(function (message) {
                    return {
                        role: message.role,
                        content: message.content,
                    };
                }),
            }

            // Use the Fetch API to make a POST request
            fetch(API_URL + '/chat/messages', {
                method: 'POST',
                headers: headers,
                body: JSON.stringify(body),
                signal: abortController.signal,
            })
                .then(response => {
                    if (!response.ok) {
                        throw new Error('Response not OK: ' + response.status);
                    }
                    // The ReadableStream is in the response.body
                    const reader = response.body.getReader();
                    return new ReadableStream({
                        start(controller) {
                            function push() {
                                const decoder = new TextDecoder();
                                reader.read().then(({ done, value }) => {
                                    if (done) {
                                        // Finish the streaming and close the reader
                                        controller.close();
                                        setIsLoading(false); // Stop loading when done
                                        return;
                                    }
                                    // Decode the stream while handling multibyte characters
                                    const text = decoder.decode(value, { stream: true })
                                        .replaceAll('data:', '');
                                    // Add streamed assistant message to conversation
                                    setOngoingConversation(conversation => {
                                        // Append the text to the last assistant message if it exists and is the last one
                                        if (conversation.length && conversation[conversation.length - 1].role === 'assistant') {
                                            return [...conversation.slice(0, -1), { ...conversation[conversation.length - 1], content: conversation[conversation.length - 1].content + text }];
                                        } else {
                                            // Otherwise, add a new assistant message
                                            return [...conversation, { role: 'assistant', content: text }];
                                        }
                                    });
                                    controller.enqueue(value);
                                    push();
                                }).catch(error => {
                                    if (error.name === 'AbortError') {
                                        console.log('Fetch aborted');
                                    } else {
                                        console.error(error);
                                        setErrorMessage('请求失败');
                                        setShowError(true);
                                    }
                                    setIsLoading(false);
                                });
                            }
                            push();
                        }
                    });
                })
                .catch(error => {
                    if (error.name === 'AbortError') {
                        console.log('Fetch aborted');
                    } else {
                        console.error(error);
                        setErrorMessage('请求失败');
                        setShowError(true);
                    }
                    setIsLoading(false);
                });
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [ongoingConversation]);

    const handleKeyPress = async (event) => {
        if (event.key === 'Enter') {
            await sendMessage();
        }
    };

    const fetchConversations = async () => {
        if (!isAuthenticated) return;

        const jwtToken = Cookies.get('jwt-token');
        const response = await fetch(`${API_URL}/chat/conversations`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${jwtToken}`,
                'Content-Type': 'application/json'
            }
        });

        if (response.ok) {
            const data = await response.json();
            setConversations(data);
        } else {
            const errorText = await response.text();
            console.error(errorText);
            setErrorMessage(`获取对话记录失败，请刷新重试`);
            setShowError(true);
            logout()
        }
    };

    const newConversation = async () => {
        setOngoingConversation([]);
    }

    const createConversation = async () => {
        if (!isAuthenticated) return;

        const jwtToken = Cookies.get('jwt-token');
        const response = await fetch(`${API_URL}/chat/conversations`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${jwtToken}`,
                'Content-Type': 'application/json'
            }
        });

        if (response.ok) {
            const newConversation = await response.json();
            setConversations([...conversations, newConversation]);
            setCurrentConversationId(newConversation.conversationId);
            return newConversation;
        } else {
            const errorText = await response.text();
            console.error(errorText);
            setErrorMessage(`创建对话记录失败，本条对话将不会被保存`);
            setShowError(true);
        }
    };

    const switchConversation = async (conversationId) => {
        setCurrentConversationId(conversationId);
        fetchMessages(conversationId).then(messages => {
            setOngoingConversation([]);
            messages.forEach(message => {
                setOngoingConversation(prevState => [...prevState, { role: message.role, content: message.content }]);
            })
            renderMessages();
        });
    }

    const deleteConversation = async (conversationId) => {
        const jwtToken = Cookies.get('jwt-token');
        const response = await fetch(`${API_URL}/chat/conversations/${conversationId}`, {
            method: 'DELETE',
            headers: {
                'Authorization': `Bearer ${jwtToken}`,
                'Content-Type': 'application/json'
            }
        });

        if (response.ok) {
            // Remove the conversation from the state
            setConversations(conversations.filter(conversation => conversation.conversationId !== conversationId));
        } else {
            // Handle error
            const errorText = await response.text();
            console.error(`Failed to delete conversation: ${errorText}`);
            setErrorMessage(`删除对话失败：${errorText}`);
            setShowError(true);
        }
    };

    const fetchMessages = async (conversationId) => {
        if (!isAuthenticated) return;

        const jwtToken = Cookies.get('jwt-token');
        const response = await fetch(`${API_URL}/chat/conversations/${conversationId}/messages`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${jwtToken}`,
                'Content-Type': 'application/json'
            }
        });

        if (response.ok) {
            return  await response.json();
        } else {
            const errorText = await response.text();
            setErrorMessage(`获取对话记录失败：${errorText}，请刷新重试`);
            setShowError(true);
        }
    };

    useEffect(() => {
        const savedUsername = Cookies.get('username');
        const savedToken = Cookies.get('jwt-token');
        if (savedUsername && savedToken) {
            setUsername(savedUsername);
            setIsAuthenticated(true);
        }
        fetchConversations();
    }, [isAuthenticated]);


    // Function to handle login/register button click
    const handleAuthButtonClick = (isLogin) => {
        setIsLoginModal(isLogin);
        setShowModal(true);
    };

    // Function to close the modal
    const handleCloseModal = () => {
        setShowModal(false);
    };

    // Function to handle form submission
    const handleFormSubmit = (data) => {
        setShowModal(false);
        if (isLoginModal) {
            login(data).then(r => {
                setInfoMessage('登录成功');
                setShowInfo(true);
            });
        } else {
            register(data).then(r => {
                setInfoMessage('注册成功');
                setShowInfo(true);
            });
        }
    };

    // Function to handle user registration
    const register = async (data) => {
        const response = await fetch(`${API_URL}/users/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data),
        });

        if (!response.ok) {
            const errorText = await response.text();
            setErrorMessage(`注册失败：${errorText}`);
            setShowError(true);
            return;
        }

        setInfoMessage('注册成功');
        setShowInfo(true);
        await login(data);
    };

    // Function to handle user login
    const login = async (data) => {
        const response = await fetch(`${API_URL}/users/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data),
        });

        if (!response.ok) {
            const errorText = await response.text();
            setErrorMessage(`登录失败：${errorText}`);
            setShowError(true);
            return;
        }

        const token = await response.text();
        setUsername(data.username);
        Cookies.set('username', data.username, { expires: 7 });
        Cookies.set('jwt-token', token, { expires: 7 });
        setIsAuthenticated(true);
    };

    // Function to handle user logout
    const logout = () => {
        setUsername('');
        Cookies.remove('username');
        Cookies.remove('jwt-token');
        setIsAuthenticated(false);
    };

    const NavigationMenu = ({ isAuthenticated, conversations, onSelectConversation, onDeleteConversation }) => {
        if (!isAuthenticated) {
            return (
                <div className="nav-menu">
                    <button className="create-conversation-button" onClick={newConversation}>
                        新对话
                    </button>
                    登录以保存对话记录
                </div>
            );
        }

        return (
            <div className="nav-menu">
                <button className="create-conversation-button" onClick={newConversation}>
                    新对话
                </button>
                {conversations.map(conversation => (
                    <div key={conversation.conversationId}
                         className={`conversation-item ${currentConversationId === conversation.conversationId ? 'active' : ''}`}
                         onClick={() => onSelectConversation(conversation.conversationId)}>
                        {conversation.title || `Conversation ${conversation.conversationId}`}
                        <button className="delete-conversation-button" onClick={(e) => {
                            e.stopPropagation();
                            onDeleteConversation(conversation.conversationId);
                        }}>
                            <i className="fas fa-trash-alt"></i>
                        </button>
                    </div>
                ))}
            </div>
        );
    };

    return (
        <div className="App">
            {showError && (
                <div className="error-message">
                    {errorMessage}
                </div>
            )}
            {showInfo && (
                <div className="info-message">
                    {infoMessage}
                </div>
            )}
            <div className="nav-menu">
                <NavigationMenu
                    isAuthenticated={isAuthenticated}
                    conversations={conversations}
                    onSelectConversation={(conversationId) => switchConversation(conversationId)}
                    onDeleteConversation={(conversationId) => deleteConversation(conversationId)}
                />
            </div>
            <div className="chat-container">
                <div className="auth-section">
                    {isAuthenticated ? (
                        <>
                            <div className="auth-description">
                                欢迎你，{username}
                            </div>
                            <div className="auth-buttons">
                                <button className="auth-button-logout" onClick={logout}>注销</button>
                            </div>
                        </>
                    ) : (
                        <>
                            <div className="auth-description">
                                登录后可以保存对话记录
                            </div>
                            <div className="auth-buttons">
                                <button className="auth-button-register"
                                        onClick={() => handleAuthButtonClick(false)}>注册
                                </button>
                                <button className="auth-button-login" onClick={() => handleAuthButtonClick(true)}>登录
                                </button>
                            </div>
                        </>
                    )}
                </div>
                {isLoading && <div className="loading-spinner"></div>}
                <div className="chat-window">
                    <div className="conversation-view">
                        {renderMessages()}
                    </div>
                    <div className="chat-input">
                        <input
                            type="text"
                            placeholder="请输入你的问题..."
                            value={inputMessage}
                            onChange={handleInputChange}
                            onKeyPress={handleKeyPress}
                        />
                        {isLoading ? (
                            <button onClick={interruptProcess}>停止生成</button>
                        ) : (
                            <button onClick={sendMessage}>发送</button>
                        )}
                    </div>
                </div>
            </div>
            <Modal
                show={showModal}
                onClose={handleCloseModal}
                isLogin={isLoginModal}
                onFormSubmit={handleFormSubmit}
            />
        </div>
    );
}

export default App;
