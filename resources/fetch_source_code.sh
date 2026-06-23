#!/bin/sh

# Replace ~ with %7E in GIT_HTTPS_URL
GIT_HTTPS_URL=$(echo "$GIT_HTTPS_URL" | sed 's/~/\%7E/g')

if [ -n "$GIT_USERNAME" ] && [ -n "$GIT_PASSWORD" ]; then
    GIT_USER=$(echo $GIT_USERNAME | sed 's/@/%40/g')
    GIT_PASS=$(echo $GIT_PASSWORD | sed 's/@/%40/g')
    HTTPS_GIT_URL_TR=$(echo $GIT_HTTPS_URL | sed 's/https:\/\///')
    git clone -b $BRANCH_NAME "https://${GIT_USER}:${GIT_PASS}@${HTTPS_GIT_URL_TR}"

elif [ -n "$PRIVATE_KEY" ]; then
    export GIT_SSH_COMMAND="ssh -i /root/*/ssh-key-private_key -o StrictHostKeyChecking=no"
    if [ -z "$GIT_FETCH" ]; then
        git clone -b $BRANCH_NAME $GIT_SSH_URL
    else
        cd $REPO_DIR
        git fetch --all && git reset --hard origin/$BRANCH_NAME
    fi

elif [ -n "$PRIVATE_KEY_LOCATION" ]; then
    export GIT_SSH_COMMAND="ssh -i $PRIVATE_KEY_LOCATION -o StrictHostKeyChecking=no"
    if [ -z "$GIT_FETCH" ]; then
        git clone -b $BRANCH_NAME $GIT_SSH_URL
    else
        cd $REPO_DIR
        git fetch --all && git reset --hard origin/$BRANCH_NAME
    fi

else
    echo "Required environment variables not found."
fi

