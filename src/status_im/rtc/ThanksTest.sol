pragma solidity ^0.4.9;

// http://ethereum.stackexchange.com/questions/729/how-to-concatenate-strings-in-solidity
import "github.com/Arachnid/solidity-stringutils/strings.sol";

contract ThanksTest {
    using strings for *;
    
    struct Voter {
        uint weight;
        bool voted;
        uint8 vote;
        address delegate;
    }
    struct Proposal {
        uint voteCount;
    }

    address chairperson;
    mapping(address => Voter) voters;
    Proposal[] proposals;

    event logtest(
        //string indexed to,
        //string indexed from,
        address from,
        address indexed to,
        uint value,
        string message);

    function () payable {
        address rtc = 0x39c4B70174041AB054f7CDb188d270Cc56D90da8;
        //if (msg.value < 10000000000000000) {
        //   throw;
        //}
        logtest(msg.sender, rtc, msg.value, "TEST_EVENT_LOGGED");
    }
    
    function sendEther(address x, string comment) {
        // http://ether.fund/tool/converter
        address rtc = 0x39c4B70174041AB054f7CDb188d270Cc56D90da8;
        address jarrad = 0x323a7fd769a0f106d6b41232b10c44064bb4be88;
        if (x == rtc) {
            rtc.send(100000000000000); // 0.0001 ETH
            logtest(msg.sender,x, 0, comment.toSlice().concat("_X".toSlice()));
        } else if (x == jarrad) {
            jarrad.send(100000000000000); // 0.0001 ETH
            logtest(msg.sender,x, 0, comment.toSlice().concat("_TO_Jarrad.".toSlice()));
        } else {
            logtest(msg.sender,x, 0, comment.toSlice().concat("_E".toSlice()));
        }
    }

    /// Give $(voter) the right to vote on this ballot.
    /// May only be called by $(chairperson).
    function giveRightToVote(address voter) {
        if (msg.sender != chairperson || voters[voter].voted) return;
        voters[voter].weight = 1;
    }

    /// Delegate your vote to the voter $(to).
    function delegate(address to) {
        Voter sender = voters[msg.sender]; // assigns reference
        if (sender.voted) return;
        while (voters[to].delegate != address(0) && voters[to].delegate != msg.sender)
            to = voters[to].delegate;
        if (to == msg.sender) return;
        sender.voted = true;
        sender.delegate = to;
        Voter delegate = voters[to];
        if (delegate.voted)
            proposals[delegate.vote].voteCount += sender.weight;
        else
            delegate.weight += sender.weight;
    }

    /// Give a single vote to proposal $(proposal).
    function vote(uint8 proposal) {
        Voter sender = voters[msg.sender];
        if (sender.voted || proposal >= proposals.length) return;
        sender.voted = true;
        sender.vote = proposal;
        proposals[proposal].voteCount += sender.weight;
    }

    function winningProposal() constant returns (uint8 winningProposal) {
        uint256 winningVoteCount = 0;
        for (uint8 proposal = 0; proposal < proposals.length; proposal++)
            if (proposals[proposal].voteCount > winningVoteCount) {
                winningVoteCount = proposals[proposal].voteCount;
                winningProposal = proposal;
            }
    }
    
    // https://is.gd/RIA00y
    function parseAddr(string _a) internal returns (address){
        bytes memory tmp = bytes(_a);
        uint160 iaddr = 0;
        uint160 b1;
        uint160 b2;
        for (uint i=2; i<2+2*20; i+=2){ iaddr *= 256; b1 = uint160(tmp[i]); b2 = uint160(tmp[i+1]); if ((b1 >= 97)&&(b1 <= 102)) b1 -= 87; else if ((b1 >= 48)&&(b1 <= 57)) b1 -= 48; if ((b2 >= 97)&&(b2 <= 102)) b2 -= 87; else if ((b2 >= 48)&&(b2 <= 57)) b2 -= 48;
            iaddr += (b1*16+b2);
        }
        return address(iaddr);
    }
}
