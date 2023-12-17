import React, { useState } from 'react';
import axios from 'axios';
import './App.css';

function App() {
    // let API_URL = 'https://simons-gpt.azurewebsites.net';
    let API_URL = 'http://localhost:8080';

    const [inputMessage, setInputMessage] = useState('');
    const [messages, setMessages] = useState([]);
    const [isLoading, setIsLoading] = useState(false);
    const [errorMessage, setErrorMessage] = useState('');
    const [showError, setShowError] = useState(false);

    const handleInputChange = (event) => {
        setInputMessage(event.target.value);
    };

    const sendMessage = () => {
        if (inputMessage.trim()) {
            setIsLoading(true); // Start loading
            setErrorMessage('');
            setShowError(false);

            // Use the Fetch API to make a POST request
            fetch(API_URL + '/chat', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ message: inputMessage }),
            })
                .then(response => {
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
                                    // Decode the stream while handling multi-byte characters
                                    const text = decoder.decode(value, { stream: true })
                                        .replaceAll('data:', '')
                                        .replaceAll('\n', '');
                                    // Append the text to the current message instead of adding it as a new message
                                    setMessages(messages => {
                                        // If there are no messages, just add the text
                                        if (messages.length === 0) {
                                            return [text];
                                        } else {
                                            // Otherwise, append the text to the last message
                                            return [...messages.slice(0, -1), messages[messages.length - 1] + text];
                                        }
                                    });
                                    controller.enqueue(value);
                                    push();
                                }).catch(error => {
                                    console.error(error);
                                    setIsLoading(false);
                                    setErrorMessage('Error receiving message: ' + error.message);
                                    setShowError(true);
                                });
                            }
                            push();

                        }
                    });
                })
                .catch(error => {
                    console.error(error);
                    setIsLoading(false);
                    setErrorMessage('Error sending message: ' + error.message);
                    setShowError(true);
                });

            setInputMessage(''); // Clear the input field
        }
    };

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
                    <div className="messages">
                        {messages.map((msg, index) => (
                            <div key={index} className="message">
                                {msg}
                            </div>
                        ))}
                    </div>
                    <div className="chat-input">
                        <input
                            type="text"
                            placeholder="请输入你的问题..."
                            value={inputMessage}
                            onChange={handleInputChange}
                            onKeyPress={handleKeyPress}
                        />
                        <button onClick={sendMessage}>发送</button>
                    </div>
                </div>
            </div>
        </div>
    );
}

export default App;
