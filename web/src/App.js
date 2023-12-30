import React, {useEffect, useState} from 'react';
import './App.css';

function App() {

    const [inputMessage, setInputMessage] = useState('');
    const [conversation, setConversation] = useState([]); // Track entire conversation
    const [isLoading, setIsLoading] = useState(false);
    const [errorMessage, setErrorMessage] = useState('');
    const [showError, setShowError] = useState(false);
    const [abortController, setAbortController] = useState(null);

    let API_URL = 'https://simons-gpt.azurewebsites.net';
    // let API_URL = 'http://localhost:8080';

    const handleInputChange = (event) => {
        setInputMessage(event.target.value);
    };

    const sendMessage = () => {
        if (inputMessage.trim()) {
            const controller = new AbortController();
            setAbortController(controller);
            setIsLoading(true); // Start loading
            setErrorMessage('');
            setShowError(false);

            // Add the user message to the conversation
            setConversation(prevState => [...prevState, { role: 'user', content: inputMessage }]);
            setInputMessage(''); // Clear the input field
        }
    };

    // Render messages with different alignment based on sender
    const renderMessages = () => {
        return conversation.map((msg, index) => (
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
        if (conversation.length > 0 && conversation[conversation.length - 1].role === 'user') {
            // Use the Fetch API to make a POST request
            fetch(API_URL + '/chat', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(conversation),
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
                                    setConversation(conversation => {
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
                                        setErrorMessage('Error receiving message: ' + error.message);
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
                        setErrorMessage('Error sending message: ' + error.message);
                        setShowError(true);
                    }
                    setIsLoading(false);
                });
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [conversation]);

    const handleKeyPress = (event) => {
        if (event.key === 'Enter') {
            sendMessage();
        }
    };

    return (
        <div className="App">
            {showError && (
                <div className="error-message">
                    {errorMessage}
                </div>
            )}
            <div className="chat-container">
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
        </div>
    );
}

export default App;
