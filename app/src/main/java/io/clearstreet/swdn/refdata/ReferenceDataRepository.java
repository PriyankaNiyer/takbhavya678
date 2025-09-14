package io.clearstreet.swdn.refdata;

import io.clearstreet.swdn.api.ReferenceDataApi;
import io.clearstreet.swdn.model.Account;
import io.clearstreet.swdn.model.Instrument;
import io.clearstreet.swdn.model.Member;

import java.util.*;

public class ReferenceDataRepository implements ReferenceDataApi {

    private static final Map<String, Member> members = new HashMap<>();
    private static final Map<String, Account> accounts = new HashMap<>();
    private static final Map<String, Instrument> instruments = new HashMap<>();
    private static final Map<String, List<String>> memberAccounts = new HashMap<>();

    public static void clearAll() {
        members.clear();
        accounts.clear();
        instruments.clear();
        memberAccounts.clear();
    }

    @Override
    public boolean enterInstrument(Instrument instrument) {
        instruments.put(instrument.instrumentName(), instrument);
        return true;
    }

    @Override
    public boolean enterAccount(Account account) {
        if (!memberAccounts.containsKey(account.memberName())) {
            List<String> accounts = new ArrayList<>();
            accounts.add(account.accountName());
            memberAccounts.put(account.memberName(), accounts);
        } else {
            memberAccounts.get(account.memberName()).add(account.accountName());
        }
        accounts.put(account.accountName(), account);
        return true;
    }

    @Override
    public boolean enterMember(Member member) {
        members.put(member.memberName(), member);
        return true;
    }

    public Optional<Instrument> getInstrument(String instrumentName) {
        return Optional.ofNullable(instruments.get(instrumentName));
    }

    public Optional<Account> getAccount(String accountName) {
        return Optional.ofNullable(accounts.get(accountName));
    }

    public Optional<Member> getMember(String memberName) {
        return Optional.ofNullable(members.get(memberName));
    }

    public List<String> getMemberAccounts(String memberName) {
        return memberAccounts.get(memberName);
    }
}
